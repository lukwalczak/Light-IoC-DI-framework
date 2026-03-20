package io.github.lukwalczak1.framework.scope.interfaces;

import java.util.Map;
import java.util.function.Supplier;

public interface Scope {
    Object get(Class<?> beanClass, Supplier<Object> objectFactory);
    Object getIfPresent(Class<?> beanClass);
    Map<Class<?>, Object> getAllInstances();
}
