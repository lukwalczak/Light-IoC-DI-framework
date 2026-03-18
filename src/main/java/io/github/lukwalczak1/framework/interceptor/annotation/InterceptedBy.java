package io.github.lukwalczak1.framework.interceptor.annotation;

import io.github.lukwalczak1.framework.interceptor.interfaces.MethodInterceptor;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD})
public @interface InterceptedBy {
    Class<? extends MethodInterceptor>[] value();
}
