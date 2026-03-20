package io.github.lukwalczak1.framework.scope.implementation;

import io.github.lukwalczak1.framework.interceptor.annotation.PreDestroy;
import io.github.lukwalczak1.framework.scope.interfaces.AbstractScope;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Request Scope implementation that manages beans within lifecycle of a single request
 * Each HTTP request will have its own instance of RequestScope bean
 */
public class RequestScope extends AbstractScope {

    private final ThreadLocal<Map<Class<?>, Object>> requestBeans = ThreadLocal.withInitial(HashMap::new);

    @Override
    public Map<Class<?>, Object> getAllInstances() {
        return requestBeans.get();
    }

    @Override
    public Object get(Class<?> beanClass, Supplier<Object> objectFactory) {
        Map<Class<?>, Object> map = requestBeans.get();
        Object existing = map.get(beanClass);
        if (existing != null) {
            return existing;
        }
        Object newInstance = objectFactory.get();
        invokePostConstructMethods(beanClass, newInstance);
        map.put(beanClass, newInstance);

        return newInstance;
    }

    @Override
    protected void invokePreDestroyMethods() {
        requestBeans.get().values().forEach(instance -> {
            try {
                Method[] methods = instance.getClass().getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(PreDestroy.class)) {
                        method.invoke(instance);
                    }
                }
            } catch (Exception e) {
                // No preDestroy method or error invoking it, ignore
            }
        });
    }

    @Override
    public Object getIfPresent(Class<?> beanClass) {
        return requestBeans.get().get(beanClass);
    }

    public void clear() {
        invokePreDestroyMethods();
        requestBeans.get().clear();
    }

}
