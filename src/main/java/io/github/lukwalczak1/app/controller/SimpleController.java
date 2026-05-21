package io.github.lukwalczak1.app.controller;
import io.github.lukwalczak1.app.exception.ExampleException;
import io.github.lukwalczak1.app.model.ExampleClass;
import io.github.lukwalczak1.app.recursiveServ.ReqServ1;
import io.github.lukwalczak1.app.recursiveServ.ReqServ2;
import io.github.lukwalczak1.app.services.*;
import io.github.lukwalczak1.app.validators.CustomValidation;
import io.github.lukwalczak1.framework.container.validation.annotation.*;
import io.github.lukwalczak1.framework.exception.annotations.ExceptionHandler;
import io.github.lukwalczak1.framework.interceptor.annotation.PostConstruct;
import io.github.lukwalczak1.framework.interceptor.annotation.PreDestroy;
import io.github.lukwalczak1.framework.web.annotations.RequestBody;
import io.github.lukwalczak1.framework.web.annotations.RequestMapping;
import io.github.lukwalczak1.framework.web.annotations.PathVariable;
import io.github.lukwalczak1.framework.container.annotations.beans.Controller;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.scope.annotation.RequestScoped;
import io.github.lukwalczak1.framework.web.response.ResponseEntity;

import java.util.List;


// Old testing controller
//@Controller
//@RequestScoped
public class SimpleController{

    @NotNull
    private SimpleService simpleService;

    @NotNull
    private ComplicatedService complicatedService;

    @NotNull
    private PrototypeService prototypeService;


    private LazyService lazyService;

    private ReqServ1 reqServ1;

    private ReqServ2 reqServ2;

    @NotNull
    private List<ServiceInterface> serviceInterfaces;

    @NotNull
    private ServiceInterface primaryInterface;

    private final Integer id = null;

    @Max(100)
    private final Float name = 50.0f;

    @Max(100)
    private final Float name2 = 10.0f;

    @CustomValidation
    private final Integer customValidatedField = 10;

    @CustomValidation
    private final Integer invalidCustomValidatedField = 12;

    @Pattern("^[a-zA-Z0-9]+$")
    private final String patternField = "ValidPattern123";

    @Pattern("^[a-zA-Z0-9]+$")
    private final String invalidPatternField = "Invalid Pattern!";

    @Valid
    private final User user = new User("asd", 10);

    protected SimpleController() {
        this.serviceInterfaces = null;
    }

    @Inject
    public SimpleController(SimpleService simpleService, ComplicatedService complicatedService, PrototypeService prototypeService, LazyService lazyService, ReqServ1 reqServ1, ReqServ2 reqServ2, List<ServiceInterface> serviceInterfaces) {
         this.complicatedService = complicatedService;
        this.simpleService = simpleService;
        this.prototypeService = prototypeService;
        this.lazyService = lazyService;
        this.reqServ1 = reqServ1;
        this.reqServ2 = reqServ2;
        this.serviceInterfaces = serviceInterfaces;
        System.out.println("serviceInterfaces size: " + serviceInterfaces.size());
        complicatedService.check();
        System.out.println("PrototypeService hashcode: " + prototypeService.hashCode());
    }
@Inject
public SimpleController(List<ServiceInterface> serviceInterfaces, ServiceInterface primaryInterface) {
    this.serviceInterfaces = serviceInterfaces;
    this.primaryInterface = primaryInterface;
}

//    @InterceptedBy({BasicInterceptor.class, LogInterceptor.class})
    @RequestMapping(value = "/example/endpoint/{id}", method = "GET")
    public ResponseEntity<String> exampleEndpoint(@PathVariable Integer id, @PathVariable Integer id_2, @PathVariable Float name) {
        lazyService.exampleMethod();
        simpleService.exampleMethod();
        System.out.println("Controller hashcode: " + this.hashCode());
        System.out.println("Service hashcode: " + simpleService.hashCode());
        return new ResponseEntity<>(200, "Hello from SimpleController!" + " Path variable id: " + id + " id_2: " + id_2 + " name: " + name);
    }

    @RequestMapping(value = "/example/endpoint/2", method = "POST")
    public ExampleClass exampleEndpoint2(@RequestBody ExampleClass requestBody) {
        System.out.println("Received request body: " + requestBody);
        return new ExampleClass("John Doe", 21);
    }

    @RequestMapping(value = "/example/endpoint/2", method = "GET")
    public ResponseEntity<ExampleClass> exampleEndpoint2() {
        simpleService.exampleMethod();
        return ResponseEntity.response(200, new ExampleClass("John Doe", 21));
    }

    @RequestMapping(value = "/example/endpoint/3", method = "GET")
    public ExampleClass  exampleEndpoint3() {
        simpleService.exampleMethod();
        return new ExampleClass("John Doe", 22);
    }

    @RequestMapping(value = "/example/endpoint/4", method = "GET")
    public void exampleEndpoint4(@PathVariable String path, @RequestBody String requestBody) {
        simpleService.exampleMethod();
    }

    @RequestMapping(value = "/example/endpoint/5", method = "GET")
    public ResponseEntity<String> exampleEndpoint5(@PathVariable String path){
        simpleService.throwMethod();
        return new ResponseEntity<>(200, "Hello from SimpleController!");
    }

    @PreDestroy
    public void preDestroyCheck() {
        System.out.println("SimpleController preDestroy called");
    }

    @PostConstruct
    public void postConstructCheck() {
        System.out.println("POST_CONSTRUCT: SimpleController postConstruct called");
    }

    @ExceptionHandler(ExampleException.class)
    public ResponseEntity<String> handleExampleException(ExampleException ex) {
        return new ResponseEntity<>(500, "An example error occurred: " + ex.getMessage());
    }


}
