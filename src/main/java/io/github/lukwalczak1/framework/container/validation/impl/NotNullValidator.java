package io.github.lukwalczak1.framework.container.validation.impl;

import io.github.lukwalczak1.framework.container.validation.annotation.NotNull;
import io.github.lukwalczak1.framework.container.validation.api.FieldValidator;
import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

public class NotNullValidator implements FieldValidator {
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof NotNull;
    }

    @Override
    public void validate(Field field, Object value, Annotation annotation) throws ValidationException {
        if (value == null) {
            throw new ValidationException("Field " + field.getName() + " cannot be null.");
        }
    }
}
