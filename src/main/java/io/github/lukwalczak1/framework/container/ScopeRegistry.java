package io.github.lukwalczak1.framework.container;

import io.github.lukwalczak1.framework.scope.annotation.ApplicationScoped;
import io.github.lukwalczak1.framework.scope.annotation.RequestScoped;
import io.github.lukwalczak1.framework.scope.annotation.SessionScoped;
import io.github.lukwalczak1.framework.scope.annotation.ViewScoped;
import io.github.lukwalczak1.framework.scope.implementation.ApplicationScope;
import io.github.lukwalczak1.framework.scope.implementation.RequestScope;
import io.github.lukwalczak1.framework.scope.implementation.SessionScope;
import io.github.lukwalczak1.framework.scope.implementation.ViewScope;
import io.github.lukwalczak1.framework.scope.interfaces.Scope;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class ScopeRegistry {

    private final Map<Class<? extends Annotation>, Scope> scopeMap = new HashMap<>();

    public ScopeRegistry() {
        scopeMap.put(ApplicationScoped.class, new ApplicationScope());
        scopeMap.put(ViewScoped.class, new ViewScope());
        scopeMap.put(RequestScoped.class, new RequestScope());
        scopeMap.put(SessionScoped.class, new SessionScope());
    }

    public Scope getScope(Class<? extends Annotation> annotationClass) {
        return scopeMap.get(annotationClass);
    }

    public <T> Scope getBeanScope(Class<T> objectClass) {
        for (Map.Entry<Class<? extends Annotation>, Scope> entry : scopeMap.entrySet()) {
            if (objectClass.isAnnotationPresent(entry.getKey())) {
                return entry.getValue();
            }
        }
        return scopeMap.get(ApplicationScoped.class);
    }
}
