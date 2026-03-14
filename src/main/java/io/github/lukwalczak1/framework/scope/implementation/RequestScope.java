package io.github.lukwalczak1.framework.scope.implementation;

import io.github.lukwalczak1.framework.scope.interfaces.AbstractScope;

import java.util.Map;
import java.util.HashMap;

public class RequestScope extends AbstractScope {

    private final ThreadLocal<Map<Class<?>, Object>> requestBeans = ThreadLocal.withInitial(HashMap::new);

    public void clear() {
        requestBeans.get().clear();
    }

}
