package com.example.msbaseprj.api.order.messaging;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Consumer that completes the end-to-end demo: it receives the events published
 * by {@link RabbitMqOutboxPublisher} from the demo queue, logs them and records
 * a {@code outbox.events.consumed} metric. Received events are also kept in
 * memory so integration tests can assert delivery. Active only under the
 * {@code rabbitmq} profile.
 */
@Component
@Profile("rabbitmq")
public class OutboxEventRabbitListener {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutboxEventRabbitListener.class);

	private final Counter consumedCounter;
	private final List<ReceivedEvent> received = new CopyOnWriteArrayList<>();

	public OutboxEventRabbitListener(MeterRegistry meterRegistry) {
		this.consumedCounter = meterRegistry.counter("outbox.events.consumed");
	}

	@RabbitListener(queues = "${app.outbox.rabbitmq.queue}")
	public void onOutboxEvent(Message message) {
		var props = message.getMessageProperties();
		var messageId = props.getMessageId();
		String eventType = props.getHeader(RabbitMqOutboxPublisher.EVENT_TYPE_HEADER);
		var body = new String(message.getBody(), StandardCharsets.UTF_8);
		received.add(new ReceivedEvent(messageId, eventType, body));
		consumedCounter.increment();
		log.info("Consumed outbox event messageId={} eventType={} body={}", messageId, eventType, body);
	}

	/** Snapshot of the events consumed so far, for test assertions. */
	public List<ReceivedEvent> receivedEvents() {
		return List.copyOf(received);
	}

	public record ReceivedEvent(String messageId, String eventType, String body) {
	}
}
