package io.github.lukwalczak1.app.services;

import io.github.lukwalczak1.app.exception.ExampleException;
import io.github.lukwalczak1.framework.container.annotations.beans.Service;
import io.github.lukwalczak1.framework.scope.annotation.ApplicationScoped;

@Service
@ApplicationScoped
public class SimpleService implements ServiceInterface {
    @Override
    public String getMessage() {
        return "";
    }

    public SimpleService() {
            System.out.println("SimpleService created.");
    }

    public void exampleMethod() {
        System.out.println("This is an example method in SimpleService.");
    }

    public void throwMethod() throws ExampleException {
        throw new ExampleException("An example exception from SimpleService.");
    }

}
