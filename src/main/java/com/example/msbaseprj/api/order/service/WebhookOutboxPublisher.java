package com.example.msbaseprj.api.order.service;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;

import com.example.msbaseprj.entity.OutboxEvent;
import com.example.msbaseprj.repository.OutboxEventRepository;

import io.micrometer.core.instrument.MeterRegistry;

@Component
@ConditionalOnProperty(prefix = "app.outbox.webhook", name = "enabled", havingValue = "true")
public class WebhookOutboxPublisher extends OutboxPublisher {
	private final RestClient webhookClient;

	public WebhookOutboxPublisher(OutboxEventRepository outboxRepository, PlatformTransactionManager transactionManager,
			MeterRegistry meterRegistry, @Qualifier("appTaskExecutor") Executor appTaskExecutor,
			@Value("${app.outbox.async-demo-enabled:false}") boolean asyncDemoEnabled,
			RestClient.Builder restClientBuilder, @Value("${app.outbox.webhook.base-url}") String webhookBaseUrl) {
		super(outboxRepository, transactionManager, meterRegistry, appTaskExecutor, asyncDemoEnabled);
		this.webhookClient = restClientBuilder.baseUrl(webhookBaseUrl).build();
	}

	@Override
	public void publishEvent(OutboxEvent event) {
		webhookClient.post().uri("/events").header("Idempotency-Key", event.getId().toString())
				.header("X-Event-Type", event.getEventType()).body(event.getPayload()).retrieve().toBodilessEntity();
	}
}