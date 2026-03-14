package io.github.lukwalczak1.framework.scope.interfaces;

import java.util.function.Supplier;

public interface Scope {
    Object get(Class<?> beanClass, Supplier<Object> objectFactory);
}
