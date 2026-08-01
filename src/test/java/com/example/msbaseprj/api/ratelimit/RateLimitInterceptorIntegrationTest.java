package com.example.msbaseprj.api.ratelimit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.msbaseprj.api.hello.service.IHelloService;

/**
 * Verifies that the Bucket4j-backed
 * {@link com.example.msbaseprj.config.web.RateLimitInterceptor} allows requests
 * up to the configured capacity and then rejects further ones with an RFC 7807
 * {@code 429 Too Many Requests} response carrying a {@code Retry-After} header.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.rate-limit.enabled=true", "app.rate-limit.capacity=3",
		"app.rate-limit.refill-period=PT1H"})
class RateLimitInterceptorIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	// Replaces the real service, which fails randomly by design, so responses are
	// deterministic.
	@MockitoBean
	private IHelloService helloService;

	@BeforeEach
	void stubService() {
		when(helloService.hello(anyString())).thenReturn("Hello, World!");
	}

	@Test
	void allowsUpToCapacityThenRejectsWithProblemDetail() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(get("/api/hello")).andExpect(status().isOk());
		}

		mockMvc.perform(get("/api/hello")).andExpect(status().isTooManyRequests())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andExpect(jsonPath("$.title").value("Too Many Requests"));
	}
}
