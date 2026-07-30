package com.example.msbaseprj.api.payment.validation;

import java.util.Currency;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Iso4217CurrencyValidator implements ConstraintValidator<Iso4217Currency, String> {
	private static final Set<Currency> SUPPORTED_CURRENCIES = Currency.getAvailableCurrencies();

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}

		try {
			return SUPPORTED_CURRENCIES.contains(Currency.getInstance(value));
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}
}