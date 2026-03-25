    package io.github.lukwalczak1.framework.container;

    import java.util.*;
    import java.lang.reflect.*;
    import java.util.stream.Collectors;

    import io.github.classgraph.*;
    import io.github.lukwalczak1.framework.container.annotations.injection.Primary;
    import io.github.lukwalczak1.framework.scope.annotation.ApplicationScoped;
    import io.github.lukwalczak1.framework.scope.annotation.Lazy;
    import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
    import io.github.lukwalczak1.framework.interceptor.annotation.PostConstruct;
    import io.github.lukwalczak1.framework.container.annotations.injection.NotNull;
    import io.github.lukwalczak1.framework.container.annotations.injection.Value;
    import io.github.lukwalczak1.framework.interceptor.annotation.InterceptedBy;
    import io.github.lukwalczak1.framework.exception.ValidationException;
    import io.github.lukwalczak1.framework.interceptor.interfaces.MethodInterceptor;
    import io.github.lukwalczak1.framework.scope.annotation.RequestScoped;
    import io.github.lukwalczak1.framework.scope.implementation.ApplicationScope;
    import io.github.lukwalczak1.framework.scope.implementation.RequestScope;
    import io.github.lukwalczak1.framework.scope.interfaces.Scope;
    import io.github.lukwalczak1.framework.web.records.MethodInvocation;
    import net.bytebuddy.ByteBuddy;
    import net.bytebuddy.implementation.InvocationHandlerAdapter;
    import net.bytebuddy.matcher.ElementMatchers;

    public class BeanFactory {

        private static BeanFactory instance;

        private Map<Class<?>, List<Class<?>>> interfaceToImpl = new HashMap<>();

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
            Set<Class<?>> candidateClasses = discoverCandidateClasses(basePackage);

            registerBeanDefinitions(candidateClasses);

            preInstantiateSingletons();

            System.out.println("BeanFactory initialized correctly");
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

        private Object wrapWithLazyProxy(Class<?> targetClass){
            try{
//                System.out.println("Creating lazy proxy for " + targetClass.getName());
                return new ByteBuddy()
                        .subclass(targetClass)
                        .method(ElementMatchers.any())
                        .intercept(InvocationHandlerAdapter.of(new LazyInterceptor(this, targetClass)))
                        .make()
                        .load(targetClass.getClassLoader())
                        .getLoaded()
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create lazy proxy for " + targetClass.getName(), e);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T getBean(Class<T> objectClass) {
            System.out.println(objectClass);
            Class<?> targetClass = resolveTargetClass(objectClass);
            System.out.println(targetClass);
            Scope beanScope = scopeRegistry.getBeanScope(targetClass);
            Object existing = beanScope.getIfPresent(targetClass);
            if (existing != null) {
                return (T) existing;
            }

            if(targetClass.isAnnotationPresent(Lazy.class)) {
                Scope scope = scopeRegistry.getBeanScope(targetClass);
                Object instance = scope.getIfPresent(targetClass);
                if(instance != null){
                    return (T) instance;
                }
                Object proxy = wrapWithLazyProxy(targetClass);
                return (T) beanScope.get(targetClass, () -> proxy);
            }

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
                List<Class<?>> implementations = interfaceToImpl.get(objectClass);
                if (implementations == null || implementations.isEmpty()) {
                    throw new RuntimeException("No implementation found for interface: " + objectClass.getName());
                }
                List<Class<?>> primaryImplementations = implementations.stream().filter(impl ->impl.isAnnotationPresent(Primary.class)).toList();
                if(primaryImplementations.size() > 1){
                    throw new RuntimeException("Multiple implementations of " + objectClass.getName() + " are marked as @Primary. Please ensure only one implementation is annotated with @Primary.");
                } else if (primaryImplementations.size() == 1) {
                    return primaryImplementations.getFirst();
                }else {
                    if(implementations.size()>1){
                        throw new RuntimeException("Multiple implementations found for interface: " + objectClass.getName() + ". Please annotate one with @Primary or inject a List of implementations.");
                    }
                    return implementations.getFirst();
                }
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
                //Check for circular dependencies
                if(parameterTypes[i].equals(targetClass)){
                    throw new RuntimeException("Circular dependency detected: " + targetClass.getName() + " depends on itself.");
                }
                // If circular dependency is detected, inject a lazy proxy instead of the actual bean
                if(checkCricularDependency(parameterTypes[i], new HashSet<>())){
                    Object dependency = scopeRegistry.getBeanScope(parameterTypes[i]).getIfPresent(parameterTypes[i]);
                    if(dependency == null){
                        dependency = wrapWithLazyProxy(parameterTypes[i]);
                        parameters[i] = dependency;
                    }else {
                        parameters[i] = dependency;
                    }
                    // Check for interfaces list
                }else if(parameterTypes[i].equals(List.class)){
                    ParameterizedType listType = (ParameterizedType) constructor.getGenericParameterTypes()[i];
                    Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
                    List<Object> implementations = new ArrayList<>();
                    for(Class<?> impl : interfaceToImpl.getOrDefault(listClass, List.of())) {
                        implementations.add(getBean(impl));
                    }
                    parameters[i] = implementations;
                }else{
                    parameters[i] = getBean(parameterTypes[i]);
                }
            }


            return constructor.newInstance(parameters);
        }

        private boolean checkCricularDependency(Class<?> targetClass, Set<Class<?>> visited) {
            if (visited.contains(targetClass)) {
                return true; // Circular dependency detected
            }
            visited.add(targetClass);
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                for (Class<?> paramType : constructor.getParameterTypes()) {
                    if (checkCricularDependency(paramType, visited)) {
                        return true;
                    }
                }
            }
            visited.remove(targetClass);
            return false;
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

        private Set<Class<?>> discoverCandidateClasses(String targetPackage) {
            List<String> annotationNames = scanForAvailableAnnotations();
//            System.out.println("Found annotations: " + annotationNames);
            Set<Class<?>> annotatedClasses = new HashSet<>();
            try (ScanResult scanResult = new ClassGraph()
                    .acceptPackages(targetPackage)
                    .enableClassInfo()
                    .enableAnnotationInfo()
                    .scan()) {

                annotationNames.forEach(annotationName -> {
                    scanResult.getAllClasses()
                            .filter(classInfo -> classInfo.hasAnnotation(annotationName))
                            .forEach(classInfo -> annotatedClasses.add(classInfo.loadClass()));
                });
            }
            return annotatedClasses;
        }

        private void registerBeanDefinitions(Set<Class<?>> candidateClasses) {
            for (Class<?> clazz : candidateClasses) {
                beanClasses.add(clazz);
                for (Class<?> iface : clazz.getInterfaces()) {
                    interfaceToImpl.computeIfAbsent(iface, k-> new ArrayList<>()).add(clazz);
                }
            }
        }

        private void preInstantiateSingletons() {
            for (Class<?> clazz : beanClasses) {
                Scope scope = scopeRegistry.getBeanScope(clazz);
                if (scope.getClass().equals(ApplicationScope.class)) {
                    //System.out.println("Pre-instantiating singleton: " + clazz.getName());
                     getBean(clazz);
                }
            }
        }

        public List<String> scanForAvailableAnnotations() {
            try (ScanResult scanResult = new ClassGraph()
                    .acceptPackages("io.github.lukwalczak1.framework.container.annotations.beans")
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

        public Set<Class<?>> getBeanClasses() {
            return beanClasses;
        }

        public Object materializeBean(Class<?> targetClass) {
            Object existing = scopeRegistry.getBeanScope(targetClass).getIfPresent(targetClass);
            if (existing != null) return existing;

            Scope beanScope = scopeRegistry.getBeanScope(targetClass);
            return beanScope.get(targetClass, () -> createBean(targetClass));
        }

    }