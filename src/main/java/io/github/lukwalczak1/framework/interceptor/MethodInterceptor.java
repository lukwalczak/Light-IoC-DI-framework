package io.github.lukwalczak1.framework.interceptor;

import io.github.lukwalczak1.framework.container.InvocationContext;

public interface MethodInterceptor {
    Object invoke(InvocationContext c) throws Throwable;
}
