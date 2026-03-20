package io.github.lukwalczak1.framework.web.records;

import java.lang.reflect.Method;

public record MethodInvocation(Object instance, java.lang.reflect.Method method) {

    public Object invoke(Object... args) throws Exception {
        return method.invoke(instance, args);
    }
    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }
}
