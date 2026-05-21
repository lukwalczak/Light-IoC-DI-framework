package io.github.lukwalczak1.app.interceptor;

import io.github.lukwalczak1.framework.container.annotations.beans.Interceptor;
import io.github.lukwalczak1.framework.container.InvocationContext;
import io.github.lukwalczak1.framework.interceptor.interfaces.MethodInterceptor;
import io.github.lukwalczak1.framework.scope.annotation.RequestScoped;


@Interceptor
@RequestScoped
public class BasicInterceptor implements MethodInterceptor {

    public BasicInterceptor() {
        System.out.println("BasicInterceptor created");
    }

    @Override
    public Object invoke(InvocationContext c) throws Throwable {
        System.out.println("Before method invocation");
        Object result = c.proceed();
        System.out.println("After method invocation");
        return result;
    }
}
