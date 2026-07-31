package com.example.msbaseprj.api.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoOutboxWebhookControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void acceptsOutboxWebhookEvent() throws Exception {
		mockMvc.perform(post("/api/demo/webhook/events").header("Idempotency-Key", "event-123")
				.header("X-Event-Type", "OrderCreated").contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderId\":42}")).andExpect(status().isNoContent());
	}
}