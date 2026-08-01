package com.example.msbaseprj.api.order.messaging;

import java.util.concurrent.Executor;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.msbaseprj.api.order.service.OutboxPublisher;
import com.example.msbaseprj.entity.OutboxEvent;
import com.example.msbaseprj.repository.OutboxEventRepository;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Outbox delivery adapter that publishes events to a RabbitMQ topic exchange.
 * Active only under the {@code rabbitmq} profile, where it is the sole outbox
 * publisher (the webhook adapter is disabled). It reuses the polling, leasing,
 * retry and dead-letter machinery inherited from {@link OutboxPublisher}.
 */
@Component
@Profile("rabbitmq")
public class RabbitMqOutboxPublisher extends OutboxPublisher {
	/** Header carrying the domain event type, mirrored on the AMQP message. */
	public static final String EVENT_TYPE_HEADER = "X-Event-Type";

	private final RabbitTemplate rabbitTemplate;
	private final String exchange;

	public RabbitMqOutboxPublisher(OutboxEventRepository outboxRepository,
			PlatformTransactionManager transactionManager, MeterRegistry meterRegistry,
			@Qualifier("appTaskExecutor") Executor appTaskExecutor,
			@Value("${app.outbox.async-demo-enabled:false}") boolean asyncDemoEnabled, RabbitTemplate rabbitTemplate,
			@Value("${app.outbox.rabbitmq.exchange}") String exchange) {
		super(outboxRepository, transactionManager, meterRegistry, appTaskExecutor, asyncDemoEnabled);
		this.rabbitTemplate = rabbitTemplate;
		this.exchange = exchange;
	}

	@Override
	public void publishEvent(OutboxEvent event) {
		rabbitTemplate.convertAndSend(exchange, event.getEventType(), event.getPayload(), message -> {
			var props = message.getMessageProperties();
			props.setMessageId(event.getId().toString());
			props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
			props.setHeader(EVENT_TYPE_HEADER, event.getEventType());
			props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			return message;
		});
	}
}
