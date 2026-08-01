package com.example.msbaseprj.api.order.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * AMQP topology for the outbox demo, declared only under the {@code rabbitmq}
 * profile. Spring's {@code RabbitAdmin} auto-declares these beans on the broker
 * at startup: a topic exchange, a durable demo queue and a catch-all binding.
 */
@Configuration(proxyBeanMethods = false)
@Profile("rabbitmq")
public class RabbitMqOutboxConfig {

	@Bean
	TopicExchange outboxExchange(@Value("${app.outbox.rabbitmq.exchange}") String exchange) {
		return new TopicExchange(exchange, true, false);
	}

	@Bean
	Queue outboxQueue(@Value("${app.outbox.rabbitmq.queue}") String queue) {
		return QueueBuilder.durable(queue).build();
	}

	@Bean
	Binding outboxBinding(Queue outboxQueue, TopicExchange outboxExchange) {
		return BindingBuilder.bind(outboxQueue).to(outboxExchange).with("#");
	}
}
