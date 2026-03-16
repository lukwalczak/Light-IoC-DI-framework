    package io.github.lukwalczak1.framework.container;

    import java.util.*;
    import java.lang.reflect.*;

    import io.github.classgraph.*;
    import io.github.lukwalczak1.framework.annotation.injection.Inject;
    import io.github.lukwalczak1.framework.annotation.PostConstruct;
    import io.github.lukwalczak1.framework.annotation.injection.NotNull;
    import io.github.lukwalczak1.framework.annotation.injection.Value;
    import io.github.lukwalczak1.framework.annotation.interceptor.InterceptedBy;
    import io.github.lukwalczak1.framework.exception.ValidationException;
    import io.github.lukwalczak1.framework.interceptor.MethodInterceptor;
    import io.github.lukwalczak1.framework.scope.annotation.RequestScoped;
    import io.github.lukwalczak1.framework.scope.implementation.RequestScope;
    import io.github.lukwalczak1.framework.scope.interfaces.Scope;
    import io.github.lukwalczak1.framework.web.MethodInvocation;
    import net.bytebuddy.ByteBuddy;
    import net.bytebuddy.implementation.InvocationHandlerAdapter;
    import net.bytebuddy.matcher.ElementMatchers;

    public class BeanFactory {
        private static BeanFactory instance;

        private Map<Class<?>, Object> beans = new HashMap<>();

        private Map<Class<?>, Class<?>> interfaceToImpl = new HashMap<>();

        private ScopeRegistry scopeRegistry = new ScopeRegistry();

        private final PropertyResolver propertyResolver = new PropertyResolver();

        private static String basePackage = "io.github.lukwalczak1";

        private Set<Class<?>> beanClasses = new HashSet<>();

        public static BeanFactory getInstance() {
            if (instance == null) {
                instance = new BeanFactory();
            }
            return instance;
        }

        public void initContext(String basePackage) {
            scanForBeans();
        }

        public Set<Class<?>> getRegisteredBeans() {
            return beans.keySet();
        }

        private void populateClassFields(Object object, Class<?> clazz){
            Class<?> currentClass = clazz;
            // Unwrapping proxy to get original class for field injection
            while (currentClass != null && currentClass != Object.class) {
                for(Field field : currentClass.getDeclaredFields()){
                    if(field.isAnnotationPresent(Inject.class)){
                        Object dependency = getBean(field.getType());
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
                    method -> method.isAnnotationPresent(InterceptedBy.class));

            if (!hasInterceptor) {
                return instance;
            }

            try {
                return new ByteBuddy()
                        .subclass(beanClass)
                        .method(ElementMatchers.any())
                        .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                            Method originalMethod = beanClass.getMethod(method.getName(), method.getParameterTypes());
                            Set<MethodInterceptor> uniqueInterceptors = new LinkedHashSet<>();
                            if (originalMethod.isAnnotationPresent(InterceptedBy.class)) {
                                Class<? extends MethodInterceptor>[] interceptorClasses =
                                        originalMethod.getAnnotation(InterceptedBy.class).value();
                                for (Class<? extends MethodInterceptor> ic : interceptorClasses) {
                                    uniqueInterceptors.add(getBean(ic));
                                }
                            }

                            MethodInvocation finalInvocation = new MethodInvocation(instance, originalMethod);
                            InvocationContext context = new InvocationContext(
                                    new ArrayList<>(uniqueInterceptors),
                                    args != null ? args : new Object[0],
                                    finalInvocation
                            );

                            return context.proceed();
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

        @SuppressWarnings("unchecked")
        public <T> T getBean(Class<T> objectClass) {
            Object bean = beans.get(objectClass);
            if (bean != null) {
                return objectClass.cast(bean);
            }

            Class<?> targetClass = resolveTargetClass(objectClass);
            Scope beanScope = scopeRegistry.getBeanScope(targetClass);

            return (T) beanScope.get(objectClass, () -> objectClass.cast(createBean(targetClass)));
        }

        private Object createBean(Class<?> targetClass) {
            try {
                // Constructor Injection
                Object instance = instantiateBean(targetClass);

                // Field Injection
                populateClassFields(instance, targetClass);

                // Validation of @NotNull fields
                validateNotNullFields(instance);

                // PostConstruct initialization
                invokePostConstruct(instance, targetClass);

                // 5. AOP Proxy
                return wrapWithProxy(instance, targetClass);

            } catch (Exception e) {
                throw new RuntimeException("Failed to create bean: " + targetClass.getName(), e);
            }
        }

        private Class<?> resolveTargetClass(Class<?> objectClass) {
            if (objectClass.isInterface()) {
                Class<?> targetClass = interfaceToImpl.get(objectClass);
                if (targetClass == null) {
                    throw new RuntimeException("No implementation found for interface: " + objectClass.getName());
                }
                return targetClass;
            }
            return objectClass;
        }

        private Object instantiateBean(Class<?> targetClass) throws Exception {
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            if (constructors.length == 0) {
                throw new RuntimeException("No public constructor found for " + targetClass.getName());
            }

            Constructor<?> constructor = Arrays.stream(constructors)
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .findFirst()
                    .orElseGet(() -> Arrays.stream(constructors)
                            .max(Comparator.comparingInt(Constructor::getParameterCount))
                            .get()
                    );
            constructor.setAccessible(true);

            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = getBean(parameterTypes[i]);
            }

            return constructor.newInstance(parameters);
        }

        private void validateNotNullFields(Object instance) throws Exception {
            for (Field f : instance.getClass().getDeclaredFields()) {
                if (f.isAnnotationPresent(NotNull.class)) {
                    f.setAccessible(true);
                    Object value = f.get(instance);
                    if (value == null) {
                        throw new ValidationException("Field " + f.getName() + " in class " +
                                instance.getClass().getName() + " is marked as @NotNull but was not injected.");
                    }
                }
            }
        }

        private void invokePostConstruct(Object instance, Class<?> targetClass) throws Exception {
            for (Method m : targetClass.getDeclaredMethods()) {
                if (m.isAnnotationPresent(PostConstruct.class)) {
                    m.setAccessible(true);
                    m.invoke(instance);
                }
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
                        getBean(interfaces[0]);
                    } else {
                        beanClasses.add(clazz);
                        getBean(clazz);
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

        public RequestScope getRequestScope() {
            return (RequestScope) scopeRegistry.getScope(RequestScoped.class);
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

        public Set<Class<?>> getBeanClasses() {
            return beanClasses;
        }

    }