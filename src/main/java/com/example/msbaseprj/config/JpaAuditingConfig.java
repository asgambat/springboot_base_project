package com.example.msbaseprj.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables Spring Data JPA auditing so entities annotated with
 * {@code @CreatedDate}, {@code @LastModifiedDate}, {@code @CreatedBy} and
 * {@code @LastModifiedBy} are populated automatically. The auditor ("who") is
 * resolved from the Spring Security context, falling back to {@code system} for
 * unauthenticated flows.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

	static final String SYSTEM_AUDITOR = "system";

	@Bean
	AuditorAware<String> auditorAware() {
		return () -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !authentication.isAuthenticated()
					|| "anonymousUser".equals(authentication.getPrincipal())) {
				return Optional.of(SYSTEM_AUDITOR);
			}
			return Optional.of(authentication.getName());
		};
	}
}
