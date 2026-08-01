package com.example.msbaseprj.api.order.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.msbaseprj.entity.OutboxEvent;
import com.example.msbaseprj.repository.OutboxEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RabbitMqOutboxPublisherTest {
	@Test
	void publishesEventToExchangeWithIdempotencyMessageId() {
		var rabbitTemplate = mock(RabbitTemplate.class);
		var publisher = new RabbitMqOutboxPublisher(mock(OutboxEventRepository.class),
				mock(PlatformTransactionManager.class), new SimpleMeterRegistry(), Runnable::run, false, rabbitTemplate,
				"outbox.events");
		var event = new OutboxEvent();
		event.setId(UUID.randomUUID());
		event.setEventType("order.created");
		event.setPayload("{\"orderId\":42}");

		publisher.publishEvent(event);

		var postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
		verify(rabbitTemplate).convertAndSend(eq("outbox.events"), eq("order.created"), eq((Object) "{\"orderId\":42}"),
				postProcessor.capture());

		var processed = postProcessor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
		var props = processed.getMessageProperties();
		assertThat(props.getMessageId()).isEqualTo(event.getId().toString());
		assertThat(props.<String>getHeader(RabbitMqOutboxPublisher.EVENT_TYPE_HEADER)).isEqualTo("order.created");
		assertThat(props.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
	}
}
