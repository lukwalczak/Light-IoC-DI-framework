package io.github.lukwalczak1.framework.container.validation.impl;

import io.github.lukwalczak1.framework.container.validation.annotation.Pattern;
import io.github.lukwalczak1.framework.container.validation.api.FieldValidator;
import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class PatternValidator implements FieldValidator {
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof Pattern;
    }

    @Override
    public void validate(Field field, Object value, Annotation annotation) throws ValidationException {
        if (value == null) {
            return;
        }
        String regexPattern = ((Pattern) annotation).value();
        if (!value.toString().matches(regexPattern)) {
            throw new ValidationException("Field " + field.getName() + " must match the pattern: " + regexPattern);
        }
    }
}
