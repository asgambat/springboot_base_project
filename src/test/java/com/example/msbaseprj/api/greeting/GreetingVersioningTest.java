package com.example.msbaseprj.api.greeting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.msbaseprj.api.greeting.controller.GreetingV1Controller;
import com.example.msbaseprj.api.greeting.controller.GreetingV2Controller;
import com.example.msbaseprj.config.web.WebSecurityConfig;

/**
 * Verifies the URI-based API versioning strategy: {@code /api/v1/greetings}
 * keeps its original single-field contract, while {@code /api/v2/greetings}
 * serves the additively enriched representation.
 */
@WebMvcTest({GreetingV1Controller.class, GreetingV2Controller.class})
@Import(WebSecurityConfig.class)
class GreetingVersioningTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void v1ReturnsOnlyMessage() throws Exception {
		mockMvc.perform(get("/api/v1/greetings").param("name", "Spring")).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Hello, Spring!"))
				.andExpect(jsonPath("$.language").doesNotExist()).andExpect(jsonPath("$.apiVersion").doesNotExist());
	}

	@Test
	void v2ReturnsEnrichedRepresentation() throws Exception {
		mockMvc.perform(get("/api/v2/greetings").param("name", "Spring").param("language", "IT"))
				.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Hello, Spring!")).andExpect(jsonPath("$.language").value("it"))
				.andExpect(jsonPath("$.apiVersion").value("v2"));
	}
}
