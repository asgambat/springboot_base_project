package com.example.msbaseprj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
	@Bean
	@Profile("demo")
	OpenAPI demoOpenAPI() {
		return openApi("Demo").addSecurityItem(new SecurityRequirement().addList("basicAuth"))
				.schemaRequirement("basicAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"));
	}

	@Bean
	@Profile("production")
	OpenAPI productionOpenAPI() {
		return openApi("Production").addSecurityItem(new SecurityRequirement().addList("bearerAuth")).schemaRequirement(
				"bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
	}

	private OpenAPI openApi(String environment) {
		return new OpenAPI().info(new Info().title("MS Base Project API - " + environment).version("v1"));
	}
}