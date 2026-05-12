package com.example.msbaseprj.api.user.model.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UpperCaseValidator implements ConstraintValidator<AllUpperCase, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) 
            return true; // Let @NotNull handle nulls

        return value.equals(value.toUpperCase());
    }

}
