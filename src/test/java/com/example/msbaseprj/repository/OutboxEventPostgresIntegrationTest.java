package com.example.msbaseprj.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.msbaseprj.entity.OutboxEvent;

@Tag("testcontainers")
@Testcontainers
@SpringBootTest
class OutboxEventPostgresIntegrationTest {
	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
	}

	@Test
	@Transactional
	void locksPendingEventsUsingPostgresSkipLocked() {
		var event = new OutboxEvent();
		event.setAggregateType("order");
		event.setAggregateId("42");
		event.setEventType("order.created");
		event.setPayload("{}");
		event.setCreatedAt(LocalDateTime.now());
		outboxEventRepository.saveAndFlush(event);

		var claimedEvents = outboxEventRepository.findAndLockPendingEvents(10, LocalDateTime.now());

		assertThat(claimedEvents).extracting(OutboxEvent::getId).containsExactly(event.getId());
	}
}