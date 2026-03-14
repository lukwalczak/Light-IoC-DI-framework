package io.github.lukwalczak1.framework.scope.interfaces;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class AbstractScope implements Scope{

    protected Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    @Override
    public Object get(Class<?> beanClass, Supplier<Object> objectFactory) {
        return instances.computeIfAbsent(beanClass, cls -> objectFactory.get());
    }
}
