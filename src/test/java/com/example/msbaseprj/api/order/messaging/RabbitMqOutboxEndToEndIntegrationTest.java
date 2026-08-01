package com.example.msbaseprj.api.order.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.msbaseprj.entity.OutboxEvent;
import com.example.msbaseprj.repository.OutboxEventRepository;

/**
 * End-to-end test of the RabbitMQ outbox path: a pending {@link OutboxEvent} is
 * persisted, the scheduled {@link RabbitMqOutboxPublisher} publishes it to the
 * broker and the {@link OutboxEventRabbitListener} consumes it. Runs only under
 * the {@code testcontainers} tag (needs Docker) and is excluded from the
 * standard suite. Postgres backs the outbox (SKIP LOCKED), RabbitMQ the broker.
 */
@Tag("testcontainers")
@Testcontainers
@SpringBootTest
@ActiveProfiles("rabbitmq")
class RabbitMqOutboxEndToEndIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Container
	static final RabbitMQContainer rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

	@Autowired
	private OutboxEventRepository outboxRepository;

	@Autowired
	private OutboxEventRabbitListener listener;

	@DynamicPropertySource
	static void configure(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
		registry.add("spring.rabbitmq.host", rabbit::getHost);
		registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
		registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
		registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
	}

	@Test
	void publishesAndConsumesOutboxEventEndToEnd() {
		var event = new OutboxEvent();
		event.setAggregateType("order");
		event.setAggregateId("42");
		event.setEventType("order.created");
		event.setPayload("{\"orderId\":42}");
		event.setCreatedAt(LocalDateTime.now());
		var saved = outboxRepository.saveAndFlush(event);

		Awaitility.await().atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> assertThat(listener.receivedEvents()).anySatisfy(received -> {
					assertThat(received.messageId()).isEqualTo(saved.getId().toString());
					assertThat(received.eventType()).isEqualTo("order.created");
					assertThat(received.body()).isEqualTo("{\"orderId\":42}");
				}));
	}
}
