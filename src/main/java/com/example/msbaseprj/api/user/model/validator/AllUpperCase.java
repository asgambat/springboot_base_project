package com.example.msbaseprj.api.user.model.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UpperCaseValidator.class)
public @interface AllUpperCase {
    String message() default "Must be all upper case";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
