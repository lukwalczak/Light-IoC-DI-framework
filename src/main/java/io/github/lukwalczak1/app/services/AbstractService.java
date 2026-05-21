package io.github.lukwalczak1.app.services;

import io.github.lukwalczak1.app.repository.SimpleRepository;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.interceptor.annotation.PostConstruct;
import io.github.lukwalczak1.framework.container.annotations.beans.Service;

@Service
public class AbstractService {

    @Inject
    protected SimpleRepository repository;

    protected AbstractService() {
        System.out.println("AbstractService created.");
    }

    protected SimpleRepository getRepository() {
        return repository;
    }

    @PostConstruct
    public void a(){
        System.out.println("AbstractService @PostConstruct method called.");
    }
}
