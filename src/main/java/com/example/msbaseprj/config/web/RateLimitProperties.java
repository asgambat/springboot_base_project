package com.example.msbaseprj.config.web;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the application-level rate limiter. Values can be
 * overridden through the {@code RATE_LIMIT_*} environment variables (see
 * {@code application.yml}).
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

	private boolean enabled = true;
	private int capacity = 20;
	private Duration refillPeriod = Duration.ofMinutes(1);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public Duration getRefillPeriod() {
		return refillPeriod;
	}

	public void setRefillPeriod(Duration refillPeriod) {
		this.refillPeriod = refillPeriod;
	}
}
