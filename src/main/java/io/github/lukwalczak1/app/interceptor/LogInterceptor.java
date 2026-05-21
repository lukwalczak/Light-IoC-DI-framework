package io.github.lukwalczak1.app.interceptor;

import io.github.lukwalczak1.framework.container.annotations.beans.Interceptor;
import io.github.lukwalczak1.framework.container.InvocationContext;
import io.github.lukwalczak1.framework.interceptor.interfaces.MethodInterceptor;
import io.github.lukwalczak1.framework.scope.annotation.RequestScoped;

@Interceptor
@RequestScoped
public class LogInterceptor implements MethodInterceptor {

    public LogInterceptor() {
    }

    @Override
    public Object invoke(InvocationContext c) throws Throwable {
        System.out.println("LogInterceptor: Before method invocation");
        Object result = c.proceed();
        System.out.println("LogInterceptor: After method invocation");
        return result;
    }
}
