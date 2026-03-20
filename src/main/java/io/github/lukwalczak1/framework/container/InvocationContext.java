package io.github.lukwalczak1.framework.container;

import io.github.lukwalczak1.framework.interceptor.interfaces.MethodInterceptor;
import io.github.lukwalczak1.framework.web.records.MethodInvocation;

import java.util.List;


public class InvocationContext {

    // This is the list of interceptors that will be invoked before the method invocation is executed
    private List<MethodInterceptor> interceptors;

    private int currentInterceptorIndex = -1;

    // Method invocation arguments
    private Object[] arguments;

    // Invocation that will be executed after all interceptors have been invoked
    private MethodInvocation methodInvocation;

    public InvocationContext(){}

    public InvocationContext(List<MethodInterceptor> interceptors, Object[] arguments, MethodInvocation methodInvocation) {
        this.interceptors = interceptors;
        this.arguments = arguments;
        this.methodInvocation = methodInvocation;
    }

    public Object proceed() throws Throwable {
        currentInterceptorIndex++;
        System.out.println(this.interceptors);
        if (currentInterceptorIndex < interceptors.size()) {
            return interceptors.get(currentInterceptorIndex).invoke(this);
        } else if (currentInterceptorIndex == interceptors.size()) {
            return methodInvocation.invoke(arguments);
        }
        return null;
    }
}
