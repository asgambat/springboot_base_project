package com.example.msbaseprj.config.web;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.micrometer.tracing.Tracer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	private final ObjectProvider<Tracer> tracerProvider;

	public WebConfig(ObjectProvider<Tracer> tracerProvider) {
		this.tracerProvider = tracerProvider;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		tracerProvider.ifAvailable(
				tracer -> registry.addInterceptor(new LogInterceptor(tracer)).addPathPatterns("/api/**").order(1));
	}

	// enables the /actuator/httpexchanges endpoint
	@Bean
	public HttpExchangeRepository httpExchangeRepository() {
		return new InMemoryHttpExchangeRepository();
	}

}
