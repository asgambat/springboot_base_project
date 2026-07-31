package com.example.msbaseprj.api.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:userlookup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class UserLookupIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsIdForDemoSeedUser() throws Exception {
		mockMvc.perform(get("/api/user").queryParam("email", "demo-outbox@example.test")).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void returnsNotFoundForUnknownEmail() throws Exception {
		mockMvc.perform(get("/api/user").queryParam("email", "unknown@example.test")).andExpect(status().isNotFound());
	}
}