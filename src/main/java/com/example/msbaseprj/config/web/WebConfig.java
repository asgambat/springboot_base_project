package com.example.msbaseprj.config.web;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.micrometer.tracing.Tracer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Tracer tracer;

    public WebConfig(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor(tracer))
                .addPathPatterns("/api/**")
                .order(1);
    }

    //enables the /actuator/httpexchanges endpoint
    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        return new InMemoryHttpExchangeRepository();
    }

}
