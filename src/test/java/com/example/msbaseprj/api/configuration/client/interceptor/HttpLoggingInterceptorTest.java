package com.example.msbaseprj.api.configuration.client.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpLoggingInterceptorTest {

	@Test
	void masksPanAndCvvInJsonPayloads() {
		var body = """
				{"cardNumber":"4111111111111111","cvv":"123","amount":19.99}
				""";

		var maskedBody = HttpLoggingInterceptor.maskSensitiveData(body);

		assertThat(maskedBody).contains("\"cardNumber\":\"***\"").contains("\"cvv\":\"***\"")
				.doesNotContain("4111111111111111").doesNotContain("\"cvv\":\"123\"");
	}
}