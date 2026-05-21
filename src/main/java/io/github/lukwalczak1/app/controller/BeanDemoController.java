package io.github.lukwalczak1.app.controller;

import io.github.lukwalczak1.app.recursiveServ.ReqServ1;
import io.github.lukwalczak1.app.recursiveServ.ReqServ2;
import io.github.lukwalczak1.app.services.LazyService;
import io.github.lukwalczak1.app.services.PrototypeService;
import io.github.lukwalczak1.app.services.ServiceInterface;
import io.github.lukwalczak1.framework.container.annotations.beans.Controller;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.interceptor.annotation.PostConstruct;
import io.github.lukwalczak1.framework.scope.annotation.RequestScoped;
import io.github.lukwalczak1.framework.web.annotations.RequestMapping;
import io.github.lukwalczak1.framework.web.response.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestScoped
public class BeanDemoController {

    private final LazyService lazyService;
    private final PrototypeService prototypeService;
    private final ReqServ1 reqServ1;
    private final ReqServ2 reqServ2;
    private final List<ServiceInterface> serviceInterfaces;
    private final ServiceInterface primaryInterface;

    @Inject
    public BeanDemoController(
            LazyService lazyService,
            PrototypeService prototypeService,
            ReqServ1 reqServ1,
            ReqServ2 reqServ2,
            List<ServiceInterface> serviceInterfaces,
            ServiceInterface primaryInterface
    ) {
        this.lazyService = lazyService;
        this.prototypeService = prototypeService;
        this.reqServ1 = reqServ1;
        this.reqServ2 = reqServ2;
        this.serviceInterfaces = serviceInterfaces;
        this.primaryInterface = primaryInterface;
    }

    @PostConstruct
    public void afterInit() {
        System.out.println("BeanDemoController initialized by custom IoC container.");
    }

    @RequestMapping(value = "/demo/beans", method = "GET")
    public ResponseEntity<Map<String, Object>> beanDemo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("controllerScope", "@RequestScoped - new controller instance per request");
        response.put("controllerHash", this.hashCode());
        response.put("prototypeServiceHash", prototypeService.hashCode());
        response.put("lazyServiceInjected", lazyService != null);
        response.put("lazyServiceClass", lazyService.getClass().getSimpleName());
        response.put("recursiveDependencyA", reqServ1.getClass().getSimpleName());
        response.put("recursiveDependencyB", reqServ2.getClass().getSimpleName());
        response.put("serviceInterfaceImplementations", serviceInterfaces.size());
        response.put("primaryImplementation", primaryInterface.getClass().getSimpleName());
        return ResponseEntity.ok(response);
    }
}