package io.github.lukwalczak1.framework.container.validation.api;

import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public interface FieldValidator {
    boolean supports(Annotation annotation);
        void validate(Field field, Object value, Annotation annotation) throws ValidationException;
}
