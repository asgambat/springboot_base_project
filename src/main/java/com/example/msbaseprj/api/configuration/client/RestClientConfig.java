package com.example.msbaseprj.api.configuration.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

import com.example.msbaseprj.api.configuration.client.HttpClientFactory.HttpClientCustomizer;
import com.example.msbaseprj.api.configuration.client.interceptor.HttpLoggingInterceptor;
import com.example.msbaseprj.api.configuration.client.interceptor.NoOpClientHttpRequestInterceptor;
import com.example.msbaseprj.api.exception.RateLimitException;

@Configuration
public class RestClientConfig {
	private static final long _defaultRetryDelayMillis = 2000L;
	private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

	private final ClientProperties externalClientProperties;

	public RestClientConfig(ClientProperties externalClientProperties) {
		this.externalClientProperties = externalClientProperties;
	}

	@Bean
	@Qualifier("externalClient")
	RestClient restClient() {
		return createBaseRestClient(externalClientProperties.getApi().isEnableLoggingInterceptor())
				.baseUrl(externalClientProperties.getApi().getBaseUrl())
				.requestFactory(createRequestFactory(externalClientProperties.getConnection())).build();
	}

	private RestClient.Builder createBaseRestClient(boolean enableLoggingInterceptor) {
		return RestClient.builder().defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.defaultStatusHandler(isTooManyRequestsError(), tooManyRequestsHandler())
				// .requestInterceptor(new TraceIdPropagationInterceptor())
				.requestInterceptor(createLoggingInterceptor(enableLoggingInterceptor));
	}

	private ClientHttpRequestInterceptor createLoggingInterceptor(boolean enableLoggingInterceptor) {
		if (enableLoggingInterceptor)
			return new HttpLoggingInterceptor();

		return new NoOpClientHttpRequestInterceptor();
	}

	private ClientHttpRequestFactory createRequestFactory(HttpClientCustomizer customizer) {
		var httpClient = HttpClientFactory.create(customizer);
		var result = new HttpComponentsClientHttpRequestFactory(httpClient);
		return new BufferingClientHttpRequestFactory(result);
	}

	private Predicate<HttpStatusCode> isTooManyRequestsError() {
		return status -> status != null && status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS);
	}

	private ErrorHandler tooManyRequestsHandler() {
		return (status, response) -> {
			long retryAfterMillis = _defaultRetryDelayMillis; // default retry after 2 seconds if header is not present
																// or cannot be parsed
			try {
				var retryAfterHeader = response.getHeaders().getFirst("Retry-After");
				if (retryAfterHeader != null)
					retryAfterMillis = Long.parseLong(retryAfterHeader) * 1000;
			} catch (Exception e) {
				// ignore and use default
				log.trace("Failed to parse Retry-After header: {}", e.getMessage(), e);
				retryAfterMillis = _defaultRetryDelayMillis;
			}
			throw new RateLimitException("Too many requests", retryAfterMillis);
		};
	}

}
