package io.github.lukwalczak1.app;

import io.github.lukwalczak1.app.exception.ExampleException;
import io.github.lukwalczak1.framework.container.annotations.beans.ControllerAdvice;
import io.github.lukwalczak1.framework.exception.annotations.ExceptionHandler;
import io.github.lukwalczak1.framework.scope.annotation.ApplicationScoped;
import io.github.lukwalczak1.framework.web.response.ResponseEntity;

@ControllerAdvice
@ApplicationScoped
public class ControllerExceptionHandler {

    public ControllerExceptionHandler() {
            System.out.println("ControllerExceptionHandler created.");
    }

    @ExceptionHandler(ExampleException.class)
    public ResponseEntity<String> handleExampleException(ExampleException ex) {
        return new ResponseEntity<>(500, "An example error occurred: handled by global exception handler" + ex.getMessage());
    }
}
