package io.github.lukwalczak1.framework.container.validation;

import io.github.lukwalczak1.framework.container.validation.api.FieldValidator;
import io.github.lukwalczak1.framework.container.validation.impl.*;
import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class ValidationEngine {

    private final Set<FieldValidator> validators = new HashSet<>();

    public ValidationEngine() {
    }

    public boolean validateField(Field f, Object o, Annotation annotation) throws ValidationException {
        for (FieldValidator validator : validators) {
            if (validator.supports(annotation)) {
                validator.validate(f, o, annotation);
            }
        }
        return true;
    }

    public void addValidator(FieldValidator validator) {
        System.out.println("Adding custom validator: " + validator.getClass().getSimpleName());
        validators.add(validator);
    }
}
