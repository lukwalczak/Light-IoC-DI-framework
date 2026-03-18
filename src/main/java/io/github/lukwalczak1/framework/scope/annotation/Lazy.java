package io.github.lukwalczak1.framework.scope.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Indicates that a bean should be lazily initialized. This means that the bean will not be created until it is first requested.
 * Lazy beans need to have a no-argument constructor for proxy creation.
 */
@Target(java.lang.annotation.ElementType.TYPE)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Lazy {
}
