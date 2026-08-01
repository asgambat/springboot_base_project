package com.example.msbaseprj.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level security annotations
 * ({@code @PreAuthorize}/{@code @PostAuthorize}) across all profiles,
 * complementing the URL-based rules in the profile-specific filter chains.
 * Fine-grained authorization can then be expressed close to the business logic.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
