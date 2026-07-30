package com.example.msbaseprj.api.payment.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Iso4217CurrencyValidator.class)
public @interface Iso4217Currency {
	String message() default "must be a valid ISO 4217 currency code";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}