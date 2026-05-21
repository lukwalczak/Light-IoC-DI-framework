package io.github.lukwalczak1.app.services;

import io.github.lukwalczak1.framework.scope.annotation.Lazy;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.scope.annotation.ApplicationScoped;

@Lazy
@ApplicationScoped
public class LazyService {

    private final PrototypeService prototypeService;

    public LazyService() {
        prototypeService = null;
        System.out.println("LazyService created without PrototypeService dependency.");
    }

    @Inject
    public LazyService(PrototypeService prototypeService) {
        this.prototypeService = prototypeService;
        System.out.println("Actual LazyService created with PrototypeService dependency.");
    }

    public void exampleMethod() {
        System.out.println("This is an example method in LazyService.");
    }
}
