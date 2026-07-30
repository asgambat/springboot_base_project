package com.example.msbaseprj.api.order.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.msbaseprj.entity.Order;
import com.example.msbaseprj.entity.OutboxEvent;
import com.example.msbaseprj.entity.User;
import com.example.msbaseprj.repository.OutboxEventRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public abstract class OutboxPublisher {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutboxPublisher.class);
	private static final int BATCH_SIZE = 100;
	private static final int MAX_ATTEMPTS = 5;
	private static final Duration LEASE_DURATION = Duration.ofSeconds(30);

	private final OutboxEventRepository outboxRepository;
	private final TransactionTemplate transactionTemplate;
	private final Counter deadLetterCounter;
	private final Counter claimedCounter;
	private final Counter publishedCounter;
	private final Counter retriedCounter;
	private final Executor appTaskExecutor;
	private final boolean asyncDemoEnabled;

	public OutboxPublisher(OutboxEventRepository outboxRepository, PlatformTransactionManager transactionManager,
			MeterRegistry meterRegistry, @Qualifier("appTaskExecutor") Executor appTaskExecutor,
			@Value("${app.outbox.async-demo-enabled:false}") boolean asyncDemoEnabled) {
		this.outboxRepository = outboxRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.deadLetterCounter = meterRegistry.counter("outbox.events.dead-lettered");
		this.claimedCounter = meterRegistry.counter("outbox.events", "outcome", "claimed");
		this.publishedCounter = meterRegistry.counter("outbox.events", "outcome", "published");
		this.retriedCounter = meterRegistry.counter("outbox.events", "outcome", "retried");
		this.appTaskExecutor = appTaskExecutor;
		this.asyncDemoEnabled = asyncDemoEnabled;
	}

	@Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
	public void publishPendingEvents() {
		for (var event : claimPendingEvents()) {
			try {
				publishEvent(event);
				if (!markProcessed(event)) {
					log.warn("Outbox event {} was no longer owned by this publisher after delivery", event.getId());
				} else {
					publishedCounter.increment();
				}
			} catch (Exception ex) {
				handlePublishFailure(event, ex);
			}
		}
	}

	@Scheduled(fixedDelayString = "${app.outbox.async-demo-interval-ms:30000}")
	public void demonstrateCompletableFutureComposition() {
		if (!asyncDemoEnabled) {
			return;
		}

		var userFuture = CompletableFuture.supplyAsync(this::getUser, appTaskExecutor);
		var ordersFuture = CompletableFuture.supplyAsync(this::getOrders, appTaskExecutor);

		CompletableFuture.allOf(userFuture, ordersFuture)
				.thenRunAsync(() -> log.debug("Async data fetch completed: user={}, orders={}",
						userFuture.join().getId(), ordersFuture.join().size()), appTaskExecutor)
				.exceptionally(this::logAsyncDemoFailure);

		userFuture.thenCombineAsync(ordersFuture, this::combine, appTaskExecutor)
				.thenAcceptAsync(
						result -> log.debug("Async combined result contains {} order(s) for user {}",
								result.values().iterator().next().size(), result.keySet().iterator().next().getId()),
						appTaskExecutor)
				.exceptionally(this::logAsyncDemoFailure);
	}

	private List<OutboxEvent> claimPendingEvents() {
		var claimedEvents = transactionTemplate.execute(status -> {
			var now = LocalDateTime.now();
			var events = outboxRepository.findAndLockPendingEvents(BATCH_SIZE, now);
			events.forEach(event -> event.claim(UUID.randomUUID(), now.plus(LEASE_DURATION)));
			return events;
		});
		if (claimedEvents != null) {
			claimedCounter.increment(claimedEvents.size());
		}
		return claimedEvents == null ? List.of() : claimedEvents;
	}

	private boolean markProcessed(OutboxEvent event) {
		var updatedRows = transactionTemplate.execute(
				status -> outboxRepository.markProcessed(event.getId(), event.getLeaseId(), LocalDateTime.now()));
		return updatedRows != null && updatedRows == 1;
	}

	private void handlePublishFailure(OutboxEvent event, Exception ex) {
		var attemptCount = event.getAttemptCount() + 1;
		var errorMessage = abbreviate(ex.getClass().getSimpleName() + ": " + ex.getMessage());

		if (attemptCount >= MAX_ATTEMPTS) {
			var movedToDeadLetter = transactionTemplate
					.execute(status -> outboxRepository.moveToDeadLetter(event.getId(), event.getLeaseId(),
							attemptCount, LocalDateTime.now(), errorMessage));
			if (movedToDeadLetter != null && movedToDeadLetter == 1) {
				deadLetterCounter.increment();
				log.error("Outbox event {} moved to dead-letter after {} attempts", event.getId(), attemptCount, ex);
			}
			return;
		}

		var retryAt = LocalDateTime.now().plus(backoffFor(attemptCount));
		transactionTemplate.executeWithoutResult(status -> outboxRepository.scheduleRetry(event.getId(),
				event.getLeaseId(), attemptCount, retryAt, errorMessage));
		retriedCounter.increment();
		log.warn("Outbox event {} failed on attempt {}; retry scheduled for {}", event.getId(), attemptCount, retryAt,
				ex);
	}

	private Duration backoffFor(int attemptCount) {
		return Duration.ofSeconds(1L << Math.min(attemptCount - 1, 6));
	}

	private String abbreviate(String errorMessage) {
		return errorMessage.length() <= 2_000 ? errorMessage : errorMessage.substring(0, 2_000);
	}

	private Map<User, List<Order>> combine(User user, List<Order> orders) {
		return Map.of(user, orders);
	}

	private User getUser() {
		return new User();
	}

	private List<Order> getOrders() {
		return List.of(new Order());
	}

	private Void logAsyncDemoFailure(Throwable ex) {
		log.error("CompletableFuture demo failed", ex);
		return null;
	}

	/** Implementations must use event.getId() as the downstream idempotency key. */
	public abstract void publishEvent(OutboxEvent event);

}
