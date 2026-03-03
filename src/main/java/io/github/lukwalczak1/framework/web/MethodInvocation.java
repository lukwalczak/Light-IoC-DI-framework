package io.github.lukwalczak1.framework.web;

public record MethodInvocation(Object instance, java.lang.reflect.Method method) {
    public Object invoke(Object... args) throws Exception {
        return method.invoke(instance, args);
    }
}
