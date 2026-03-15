package io.github.lukwalczak1.framework.scope.implementation;

import io.github.lukwalczak1.framework.scope.interfaces.AbstractScope;

import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

public class RequestScope extends AbstractScope {

    private final ThreadLocal<Map<Class<?>, Object>> requestBeans = ThreadLocal.withInitial(HashMap::new);

    @Override
    public Object get(Class<?> beanClass, Supplier<Object> objectFactory) {
        Map<Class<?>, Object> map = requestBeans.get();
        Object existing = map.get(beanClass);
        if (existing != null) {
            return existing;
        }
        Object newInstance = objectFactory.get();
        map.put(beanClass, newInstance);

        return newInstance;
    }

    public void clear() {
        requestBeans.get().clear();
    }

}
