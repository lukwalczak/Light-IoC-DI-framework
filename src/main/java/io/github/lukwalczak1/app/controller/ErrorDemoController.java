package io.github.lukwalczak1.app.controller;

import io.github.lukwalczak1.app.services.SimpleService;
import io.github.lukwalczak1.framework.container.annotations.beans.Controller;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.exception.annotations.ExceptionHandler;
import io.github.lukwalczak1.framework.web.annotations.RequestMapping;
import io.github.lukwalczak1.framework.web.response.ResponseEntity;

@Controller
public class ErrorDemoController {

    private final SimpleService simpleService;

    @Inject
    public ErrorDemoController(SimpleService simpleService) {
        this.simpleService = simpleService;
    }

    @RequestMapping(value = "/demo/errors/local", method = "GET")
    public ResponseEntity<String> localError() {
        throw new IllegalArgumentException("This exception is handled inside ErrorDemoController.");
    }

    @RequestMapping(value = "/demo/errors/global", method = "GET")
    public ResponseEntity<String> globalError() {
        simpleService.throwMethod();
        return ResponseEntity.ok("This line will not be reached.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.response(
                400,
                "Handled locally by controller: " + ex.getMessage()
        );
    }
}