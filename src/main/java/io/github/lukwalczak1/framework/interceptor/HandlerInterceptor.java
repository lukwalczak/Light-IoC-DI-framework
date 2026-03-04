package io.github.lukwalczak1.framework.interceptor;

public interface HandlerInterceptor extends IBasicInterceptor {

    boolean preHandle(Object handler, Object... args);

    void postHandle(Object handler, Object... args);
}
