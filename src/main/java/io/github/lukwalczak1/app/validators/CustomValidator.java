package io.github.lukwalczak1.app.validators;

import io.github.lukwalczak1.framework.container.validation.annotation.Min;
import io.github.lukwalczak1.framework.container.validation.api.FieldValidator;
import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class CustomValidator implements FieldValidator {
    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof CustomValidation;
    }

    @Override
    public void validate(Field field, Object value, Annotation annotation) throws ValidationException {
        if(value == null) {
            return;
        }
        if(!(value instanceof Number)) {
            throw new ValidationException("Field " + field.getName() + " must be a number for Min validation.");
        }
        long val = ((Min) annotation).value();
        if(val == 12){
            throw new ValidationException("Value cannot be 12 for field " + field.getName());
        }
    }
}
