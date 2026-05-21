package io.github.lukwalczak1.framework.container;

import java.lang.reflect.Method;

public class LazyInterceptor implements java.lang.reflect.InvocationHandler {

    private final BeanFactory beanFactory;
    private final Class<?> targetClass;

    private Object targetInstance;

    public LazyInterceptor(BeanFactory beanFactory, Class<?> targetClass) {
        this.beanFactory = beanFactory;
        this.targetClass = targetClass;
    }

    @Override
    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (targetInstance == null) {
            targetInstance = beanFactory.createLazyTarget(targetClass);
        }

        return method.invoke(targetInstance, args);
    }
}