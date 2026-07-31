package com.example.msbaseprj.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("demo")
class DemoFlywaySeedIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void loadsOutboxUserFromDemoFlywayDataset() {
		assertThat(userRepository.findByEmail("demo-outbox@example.test")).isPresent();
	}
}