package io.github.lukwalczak1.framework.container;

import java.util.*;
import java.lang.reflect.*;

import io.github.classgraph.*;
import io.github.lukwalczak1.framework.annotation.injection.Inject;
import io.github.lukwalczak1.framework.annotation.PostConstruct;
import io.github.lukwalczak1.framework.annotation.injection.Value;
import io.github.lukwalczak1.framework.annotation.interceptor.PostInvoke;
import io.github.lukwalczak1.framework.annotation.interceptor.PreInvoke;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

public class BeanFactory {
    private static BeanFactory instance;

    private Map<Class<?>, Object> beans = new HashMap<>();

    private Map<Class<?>, Class<?>> interfaceToImpl = new HashMap<>();

    private static String basePackage = "io.github.lukwalczak1";

    private final PropertyResolver propertyResolver = new PropertyResolver();

    public static BeanFactory getInstance() {
        if (instance == null) {
            instance = new BeanFactory();
        }
        return instance;
    }

    public void initContext(String basePackage) {
        scanForBeans();
    }


    private void registerBean(Class<?> objectClass, Object instance) {
        beans.put(objectClass, instance);
        System.out.println("Registered bean: " + objectClass.getName());
    }

    public Set<Class<?>> getRegisteredBeans() {
        return beans.keySet();
    }

    public <T> T getBean(Class<T> objectClass) {
        return (T) beans.get(objectClass);
    }

    private void populateClassFields(Object object, Class<?> clazz){
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for(Field field : currentClass.getDeclaredFields()){
                if(field.isAnnotationPresent(Inject.class)){
                    Object dependency = getOrCreateBean(field.getType());
                    injectValue(field, object, dependency);
                } else if (field.isAnnotationPresent(Value.class)) {
                    String key = field.getAnnotation(Value.class).value();
                    String value = propertyResolver.resolve(key);
                    if (value != null) {
                        injectValue(field, object, convert(field.getType(), value));
                    }
                }
            }
        currentClass = currentClass.getSuperclass();
        }
    }

    private Object convert(Class<?> targetType, String value){
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private void injectValue(Field field, Object target, Object value){
        try {
            field.setAccessible(true);
            field.set(target, value);
        }catch (Exception e){
            throw new RuntimeException("Failed to inject dependency: " + field.getType().getName() + " into " + target.getClass(), e);
        }
    }
    private Object wrapWithProxy(Object instance, Class<?> beanClass) {
        boolean hasInterceptor = Arrays.stream(beanClass.getDeclaredMethods()).anyMatch(
                method -> method.isAnnotationPresent(PreInvoke.class) || method.isAnnotationPresent(PostInvoke.class)
        );

        if (!hasInterceptor) {
            return instance;
        }

        try {
            return new ByteBuddy()
                    .subclass(beanClass)
                    .method(ElementMatchers.any())
                    .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                        Method originalMethod = beanClass.getMethod(method.getName(), method.getParameterTypes());

                        if (originalMethod.isAnnotationPresent(PreInvoke.class)) {
                            Class<?> interceptorClass = originalMethod.getAnnotation(PreInvoke.class).value();
                            Object interceptorInstance = getOrCreateBean(interceptorClass);
                            Method interceptorMethod = interceptorClass.getMethod("intercept", Object[].class);
                            Object[] finalArgs = (args == null) ? new Object[0] : args;
                            interceptorMethod.invoke(interceptorInstance, (Object) finalArgs);
                        }

                        originalMethod.setAccessible(true);
                        Object result = originalMethod.invoke(instance, args);

                        if (originalMethod.isAnnotationPresent(PostInvoke.class)) {
                            Class<?> interceptorClass = originalMethod.getAnnotation(PostInvoke.class).value();
                            Object interceptorInstance = getOrCreateBean(interceptorClass);
                            Method interceptorMethod = interceptorClass.getMethod("intercept", Object[].class);
                            Object[] finalArgs = (args == null) ? new Object[0] : args;
                            interceptorMethod.invoke(interceptorInstance, (Object) finalArgs);
                        }

                        return result;
                    }))
                    .make()
                    .load(beanClass.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("ByteBuddy failed to create proxy for " + beanClass.getName(), e);
        }
    }

    private <T> T getOrCreateBean(Class<T> objectClass) {
        if (beans.containsKey(objectClass)) {
            return objectClass.cast(beans.get(objectClass));
        }

        Class<?> targetClass = objectClass;
        if (objectClass.isInterface()) {
            targetClass = interfaceToImpl.get(objectClass);
            if (targetClass == null) {
                throw new RuntimeException("No implementation found for interface: " + objectClass.getName());
            }
        }

        try {
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            if (constructors.length == 0) {
                throw new RuntimeException("No public constructor found for " + targetClass.getName());
            }
            Constructor<?> constructor = Arrays.stream(constructors)
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .findFirst()
                    .orElseGet(() -> Arrays.stream(constructors)
                            .max(Comparator.comparingInt(c -> c.getParameterCount()))
                            .get()
                    );
            constructor.setAccessible(true);

            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = getOrCreateBean(parameterTypes[i]);
            }

            Object instance = constructor.newInstance(parameters);

            // Field Injection
            populateClassFields(instance, targetClass);

            // PostConstruct
            for (Method m : targetClass.getDeclaredMethods()) {
                if (m.isAnnotationPresent(PostConstruct.class)) {
                    m.setAccessible(true);
                    m.invoke(instance);
                }
            }


            // AOP Proxy
            Object proxyInstance = wrapWithProxy(instance, targetClass);

            beans.put(objectClass, proxyInstance);
            return objectClass.cast(proxyInstance);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + objectClass.getName(), e);
        }
    }

    private void scanForBeans() {
        List<String> annotationNames = scanForAvailableAnnotations();
        System.out.println("Found annotations: " + annotationNames);
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(basePackage)
                .enableClassInfo()
                .enableAnnotationInfo()
                .scan()) {
            Set<Class<?>> annotatedClasses = new HashSet<>();
            annotationNames.forEach( annotationName -> {
               scanResult.getAllClasses()
                       .filter(classInfo -> classInfo.hasAnnotation(annotationName))
                       .forEach(classInfo -> {
                           annotatedClasses.add(classInfo.loadClass());
                       });
            });
            for(Class<?> clazz : annotatedClasses) {
                for(Class<?> iface : clazz.getInterfaces()){
                    interfaceToImpl.put(iface, clazz);
                }
            }
            for (Class<?> clazz : annotatedClasses) {
                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces.length > 0) {
                    getOrCreateBean(interfaces[0]);
                } else {
                    getOrCreateBean(clazz);
                }
            }
        }
    }

    public List<String> scanForAvailableAnnotations() {
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages("io.github.lukwalczak1.framework.annotation.beans")
                .enableClassInfo()
                .scan()) {
            return scanResult
                    .getAllClasses()
                    .filter(ClassInfo::isAnnotation)
                    .getNames();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }

    }

    public <T> List<T> getBeansOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Map.Entry<Class<?>, Object> entry : beans.entrySet()) {
            Class<?> beanClass = entry.getKey();
            Object beanInstance = entry.getValue();

            if (type.isAssignableFrom(beanClass)) {
                result.add(type.cast(beanInstance));
            }
        }
        return result;
    }

}