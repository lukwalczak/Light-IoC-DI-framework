package io.github.lukwalczak1.app.services;

import io.github.lukwalczak1.framework.container.annotations.beans.Service;
import io.github.lukwalczak1.framework.scope.annotation.PrototypeScoped;


@PrototypeScoped
@Service
public class PrototypeService {
    public PrototypeService() {
        System.out.println("Creating a new instance of PrototypeService");
        System.out.println("Instance hash code: " + this.hashCode());
    }

}
