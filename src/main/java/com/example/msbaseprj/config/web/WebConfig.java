package com.example.msbaseprj.config.web;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	private final ObjectProvider<Tracer> tracerProvider;
	private final ObjectProvider<RateLimitProperties> rateLimitPropertiesProvider;
	private final ObjectProvider<ObjectMapper> objectMapperProvider;
	private final ObjectProvider<MeterRegistry> meterRegistryProvider;

	public WebConfig(ObjectProvider<Tracer> tracerProvider,
			ObjectProvider<RateLimitProperties> rateLimitPropertiesProvider,
			ObjectProvider<ObjectMapper> objectMapperProvider, ObjectProvider<MeterRegistry> meterRegistryProvider) {
		this.tracerProvider = tracerProvider;
		this.rateLimitPropertiesProvider = rateLimitPropertiesProvider;
		this.objectMapperProvider = objectMapperProvider;
		this.meterRegistryProvider = meterRegistryProvider;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		var rateLimitProperties = rateLimitPropertiesProvider.getIfAvailable();
		var objectMapper = objectMapperProvider.getIfAvailable();
		var meterRegistry = meterRegistryProvider.getIfAvailable();
		if (rateLimitProperties != null && objectMapper != null && meterRegistry != null) {
			registry.addInterceptor(new RateLimitInterceptor(rateLimitProperties, objectMapper, meterRegistry))
					.addPathPatterns("/api/**").order(0);
		}
		tracerProvider.ifAvailable(
				tracer -> registry.addInterceptor(new LogInterceptor(tracer)).addPathPatterns("/api/**").order(1));
	}

	// enables the /actuator/httpexchanges endpoint
	@Bean
	public HttpExchangeRepository httpExchangeRepository() {
		return new InMemoryHttpExchangeRepository();
	}

}
