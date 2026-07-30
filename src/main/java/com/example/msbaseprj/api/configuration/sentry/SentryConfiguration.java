package com.example.msbaseprj.api.configuration.sentry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.msbaseprj.api.exception.NotFoundException;

import io.sentry.SentryOptions;
import jakarta.validation.ValidationException;

@Configuration
public class SentryConfiguration {

	// Don't send expected, non-actionable exceptions to Sentry
	@Bean
	public SentryOptions.BeforeSendCallback beforeSend() {
		return (event, hint) -> {
			// Ignore 404s and validation errors — these are expected
			if (event.getThrowable() instanceof NotFoundException)
				return null;
			if (event.getThrowable() instanceof ValidationException)
				return null;
			return event;
		};
	}

}
