package io.github.lukwalczak1.app.services;

import io.github.lukwalczak1.app.repository.SimpleRepository;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.container.annotations.beans.Service;
import io.github.lukwalczak1.framework.container.annotations.injection.Primary;
import io.github.lukwalczak1.framework.scope.annotation.ApplicationScoped;

@Service
@ApplicationScoped
@Primary
public class ComplicatedService implements ServiceInterface {
    @Override
    public String getMessage() {
        return "";
    }

    private final SimpleService simpleService;

    private final SimpleRepository simpleRepository;

    private final PrototypeService prototypeService;

    @Inject
     public ComplicatedService(SimpleService simpleService, SimpleRepository simpleRepository, PrototypeService prototypeService) {
         this.simpleService = null;
         this.simpleRepository = null;
        this.prototypeService = null;
        System.out.println("PrototypeService hashcode" + prototypeService.hashCode());
     }

     public void exampleMethod() {
         System.out.println("This is an example method in ComplicatedService.");
     }

     public void check(){
         System.out.println("Controller PrototypeService hashcode: " + prototypeService.hashCode());
     }
}
