package io.github.lukwalczak1.framework.scope.interfaces;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class AbstractScope implements Scope {

    protected Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    @Override
    public Object get(Class<?> beanClass, Supplier<Object> objectFactory) {
        Object instance = instances.get(beanClass);

        if (instance == null) {
            synchronized (this) {
                instance = instances.get(beanClass);
                if (instance == null) {
                    instance = objectFactory.get();
                    invokePostConstructMethods(beanClass, instance);
                    instances.put(beanClass, instance);
                }
            }
        }
        return instance;
    }

    @Override
    public Object getIfPresent(Class<?> beanClass) {
        return instances.get(beanClass);
    }

    protected void invokePostConstructMethods(Class<?> beanClass, Object instance) {
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(io.github.lukwalczak1.framework.annotation.interceptor.PostConstruct.class)) {
                try {
                    method.invoke(instance);
                } catch (Exception e) {
                    // No postConstruct method or error invoking it, ignore
                }
            }
        }
    }

    protected void invokePreDestroyMethods(){
        instances.values().forEach(instance -> {
            try {
                Method[] methods = instance.getClass().getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(io.github.lukwalczak1.framework.annotation.interceptor.PreDestroy.class)) {
                        method.invoke(instance);
                    }
                }
            } catch (Exception e) {
                // No preDestroy method or error invoking it, ignore
            }
        });
    }

    public void clear() {
        invokePreDestroyMethods();
        instances.clear();
    }
}