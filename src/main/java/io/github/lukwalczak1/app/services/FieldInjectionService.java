package io.github.lukwalczak1.app.services;

import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.interceptor.annotation.PostConstruct;
import io.github.lukwalczak1.framework.container.annotations.beans.Service;
import io.github.lukwalczak1.framework.container.annotations.injection.Value;
import io.github.lukwalczak1.framework.scope.annotation.ApplicationScoped;

@Service
@ApplicationScoped
public class FieldInjectionService extends AbstractService {

    @Inject
    public SimpleService simpleService;

    @Value("app.service.maxValue")
    public int maxValue;

    protected FieldInjectionService() {
        System.out.println("FieldInjectionService created with field injection.");

    }
    @PostConstruct
    public void a(){
        System.out.println(maxValue);
        if(simpleService != null) {
            System.out.println("SimpleService injected successfully in FieldInjectionService.");
        } else {
            System.out.println("SimpleService injection failed in FieldInjectionService.");
        }
        if(super.getRepository() != null) {
            System.out.println("SimpleRepository injected successfully in AbstractService.");
        } else {
            System.out.println("SimpleRepository injection failed in AbstractService.");
        }

    }
}
