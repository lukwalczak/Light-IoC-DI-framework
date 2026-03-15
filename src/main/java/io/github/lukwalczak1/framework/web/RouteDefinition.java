package io.github.lukwalczak1.framework.web;

import java.lang.reflect.Method;

public record RouteDefinition(Class<?> controllerClass, Method method) {
}
