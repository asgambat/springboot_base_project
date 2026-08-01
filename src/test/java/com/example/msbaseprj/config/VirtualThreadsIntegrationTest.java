package com.example.msbaseprj.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confirms that {@code spring.threads.virtual.enabled=true} makes Tomcat serve
 * each request on a virtual thread. A test-only controller reports whether its
 * request thread is virtual.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VirtualThreadsIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void requestsAreServedOnVirtualThreads() {
		String body = restTemplate.getForObject("http://localhost:" + port + "/api/internal/thread-kind", String.class);

		assertThat(body).isEqualTo("virtual");
	}

	@TestConfiguration
	static class ThreadKindTestConfig {

		@Bean
		ThreadKindController threadKindController() {
			return new ThreadKindController();
		}
	}

	@RestController
	static class ThreadKindController {

		@GetMapping("/api/internal/thread-kind")
		String threadKind() {
			return Thread.currentThread().isVirtual() ? "virtual" : "platform";
		}
	}
}
