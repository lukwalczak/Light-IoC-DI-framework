package io.github.lukwalczak1.framework.container.annotations.beans;

import io.github.lukwalczak1.framework.container.annotations.Bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Bean
public @interface Interceptor {
}

