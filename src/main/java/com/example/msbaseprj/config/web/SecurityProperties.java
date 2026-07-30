package com.example.msbaseprj.config.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Validated
public class SecurityProperties {

	@NotBlank
	private String apiKey;

	@Min(1)
	@Max(60)
	private int sessionTimeoutInMinutes;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public int getSessionTimeoutInMinutes() {
		return sessionTimeoutInMinutes;
	}

	public void setSessionTimeoutInMinutes(int sessionTimeoutInMinutes) {
		this.sessionTimeoutInMinutes = sessionTimeoutInMinutes;
	}

}
