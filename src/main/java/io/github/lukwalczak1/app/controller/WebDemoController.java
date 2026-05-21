package io.github.lukwalczak1.app.controller;

import io.github.lukwalczak1.app.interceptor.BasicInterceptor;
import io.github.lukwalczak1.app.interceptor.LogInterceptor;
import io.github.lukwalczak1.app.model.ExampleClass;
import io.github.lukwalczak1.app.services.SimpleService;
import io.github.lukwalczak1.framework.container.annotations.beans.Controller;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.interceptor.annotation.InterceptedBy;
import io.github.lukwalczak1.framework.web.annotations.PathVariable;
import io.github.lukwalczak1.framework.web.annotations.RequestBody;
import io.github.lukwalczak1.framework.web.annotations.RequestMapping;
import io.github.lukwalczak1.framework.web.response.ResponseEntity;

@Controller
public class WebDemoController {

    private final SimpleService simpleService;

    protected WebDemoController() {
        this.simpleService = null;
    }

    @Inject
    public WebDemoController(SimpleService simpleService) {
        this.simpleService = simpleService;
    }

    @RequestMapping(value = "/demo/hello/{id}", method = "GET")
    public ResponseEntity<String> hello(@PathVariable("id") Integer id){
        simpleService.exampleMethod();
        return ResponseEntity.ok("Hello from custom framework. Path variable id = " + id);
    }

    @RequestMapping(value = "/demo/body", method = "POST")
    public ResponseEntity<ExampleClass> requestBodyDemo(@RequestBody ExampleClass requestBody) {
        System.out.println("Received request body: " + requestBody.getName() + ", " + requestBody.getValue());

        ExampleClass response = new ExampleClass(
                "Received: " + requestBody.getName(),
                requestBody.getValue()
        );

        return ResponseEntity.ok(response);
    }

    @InterceptedBy({BasicInterceptor.class, LogInterceptor.class})
    @RequestMapping(value = "/demo/interceptors", method = "GET")
    public ResponseEntity<String> interceptorDemo() {
        simpleService.exampleMethod();
        return ResponseEntity.ok("This endpoint was wrapped by BasicInterceptor and LogInterceptor.");
    }
}