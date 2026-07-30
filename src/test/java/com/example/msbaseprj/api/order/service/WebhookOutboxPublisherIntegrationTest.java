package com.example.msbaseprj.api.order.service;

import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

import com.example.msbaseprj.entity.OutboxEvent;
import com.example.msbaseprj.repository.OutboxEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class WebhookOutboxPublisherIntegrationTest {
	@Test
	void publishesEventToWebhookWithDownstreamIdempotencyKey() {
		var restClientBuilder = RestClient.builder();
		var mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		var publisher = new WebhookOutboxPublisher(mock(OutboxEventRepository.class),
				mock(PlatformTransactionManager.class), new SimpleMeterRegistry(), Runnable::run, false,
				restClientBuilder, "https://webhook.example");
		var event = new OutboxEvent();
		event.setId(UUID.randomUUID());
		event.setEventType("order.created");
		event.setPayload("{\"orderId\":42}");

		mockServer.expect(requestTo("https://webhook.example/events")).andExpect(method(POST))
				.andExpect(header("Idempotency-Key", event.getId().toString()))
				.andExpect(header("X-Event-Type", "order.created")).andExpect(content().string("{\"orderId\":42}"))
				.andRespond(withNoContent());

		publisher.publishEvent(event);

		mockServer.verify();
	}
}