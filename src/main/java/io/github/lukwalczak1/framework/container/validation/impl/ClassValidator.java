package io.github.lukwalczak1.framework.container.validation.impl;

import io.github.lukwalczak1.framework.container.annotations.Bean;
import io.github.lukwalczak1.framework.container.validation.annotation.Valid;
import io.github.lukwalczak1.framework.container.validation.api.FieldValidator;
import io.github.lukwalczak1.framework.exception.ValidationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

public class ClassValidator implements FieldValidator {

    private final List<FieldValidator> validators = List.of(
        new NotNullValidator(),
        new MinValidator(),
        new MaxValidator(),
        new PatternValidator()
    );

    @Override
    public boolean supports(Annotation annotation) {
        return annotation instanceof Valid;
    }

    @Override
    public void validate(Field field, Object value, Annotation annotation) throws ValidationException {
        System.out.println("Validating field: " + field.getName() + " with @Valid");
        if (value == null) {
            return;
        }

        if(field.getType().isPrimitive() || field.getType().isEnum() || field.getType().isArray()) {
            throw new ValidationException("Field " + field.getName() + " must be an object for Valid validation.");
        }
        if(value.getClass().isAnnotationPresent(Bean.class)) {
            throw new ValidationException("Field " + field.getName() + " must not be a bean for Valid validation.");
        }

        for(Field f : value.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            System.out.println("Validating field: " + f.getName());
            for(Annotation a : f.getAnnotations()){
                for(FieldValidator validator : validators){
                    try {
                        if(validator.supports(a)){
                            validator.validate(f, f.get(value), a);
                        }
                    } catch (ValidationException e) {
                        throw new ValidationException("Validation failed for field " + f.getName() + ": " + e.getMessage());
                    } catch (Exception e){
                        throw new ValidationException("Error accessing field " + f.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

    }
}
