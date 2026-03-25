package io.github.lukwalczak1.framework.container.validation.impl;

import io.github.lukwalczak1.framework.container.validation.annotation.Max;
import io.github.lukwalczak1.framework.container.validation.api.FieldValidator;
import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class MaxValidator implements FieldValidator {
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof Max;
    }

    @Override
    public void validate(Field field, Object value, Annotation annotation) throws ValidationException {
        if(value == null) {
            return;
        }
        if(!Number.class.isAssignableFrom(field.getType())) {
            throw new ValidationException("Field " + field.getName() + " must be a number for Max validation.");
        }
        long maxValue = ((Max) annotation).value();
        long fieldValue = ((Number) value).longValue();
        if (fieldValue > maxValue) {
            throw new ValidationException("Field " + field.getName() + " must be at most " + maxValue + ".");
        }
    }
}
