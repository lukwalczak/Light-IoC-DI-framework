package io.github.lukwalczak1.framework.scope.interfaces;

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
                    instances.put(beanClass, instance);
                }
            }
        }
        return instance;
    }

    protected void InvokePreDestroyMethods(){
        instances.values().forEach(instance -> {
            try {
                instance.getClass().getMethod("preDestroy").invoke(instance);
            } catch (Exception e) {
                // No preDestroy method or error invoking it, ignore
            }
        });
    }
}