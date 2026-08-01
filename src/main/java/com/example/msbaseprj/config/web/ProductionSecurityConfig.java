package com.example.msbaseprj.config.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile("production")
public class ProductionSecurityConfig {

	@Bean
	SecurityFilterChain productionSecurityFilterChain(HttpSecurity http,
			CorsConfigurationSource corsConfigurationSource) throws Exception {
		http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource))
				.headers(headers -> headers.frameOptions(frame -> frame.deny())
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
						.httpStrictTransportSecurity(
								hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000))
						.contentSecurityPolicy(
								csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.authorizeHttpRequests(requests -> requests.requestMatchers("/actuator/health/**", "/actuator/info")
						.permitAll().requestMatchers("/actuator/**").hasAuthority("SCOPE_actuator.read")
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/payments/**", "/api/secured/**").authenticated().requestMatchers("/api/**")
						.permitAll().anyRequest().denyAll());

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.security.cors.allowed-origins}") List<String> allowedOrigins) {
		var configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "traceparent"));
		configuration.setExposedHeaders(List.of("Retry-After", "traceparent"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}