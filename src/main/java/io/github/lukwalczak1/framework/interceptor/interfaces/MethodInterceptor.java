package io.github.lukwalczak1.framework.interceptor.interfaces;

import io.github.lukwalczak1.framework.container.InvocationContext;

public interface MethodInterceptor {
    Object invoke(InvocationContext c) throws Throwable;
}
