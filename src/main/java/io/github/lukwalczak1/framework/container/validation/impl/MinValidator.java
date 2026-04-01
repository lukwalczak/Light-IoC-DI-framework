package io.github.lukwalczak1.framework.container.validation.impl;

import io.github.lukwalczak1.framework.container.validation.annotation.Min;
import io.github.lukwalczak1.framework.container.validation.api.FieldValidator;
import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class MinValidator implements FieldValidator {
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof Min;
    }

    @Override
    public void validate(Field field, Object value, Annotation annotation) throws ValidationException {
        if(value == null) {
            return;
        }
        if(!(value instanceof Number)) {
            throw new ValidationException("Field " + field.getName() + " must be a number for Min validation.");
        }
        long minValue = ((Min) annotation).value();
        long fieldValue = ((Number) value).longValue();
        if (fieldValue < minValue) {
            throw new ValidationException("Field " + field.getName() + " must be at least " + minValue + ".");
        }
    }
}
