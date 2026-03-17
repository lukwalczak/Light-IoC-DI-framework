package io.github.lukwalczak1.framework.scope.implementation;

import io.github.lukwalczak1.framework.scope.interfaces.AbstractScope;

import java.util.function.Supplier;

/**
 * PrototypeScope creates a new instance of a benan every time it is injected
 */
public class PrototypeScope extends AbstractScope {
        @Override
        public Object get(Class<?> beanClass, Supplier<Object> objectFactory) {
            Object instance = objectFactory.get();
            invokePostConstructMethods(beanClass, instance);
            return instance;
        }
}
