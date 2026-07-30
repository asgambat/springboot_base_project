package com.example.msbaseprj.api.secured.balance.service;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class BalanceMetric {
	// A counter to track balance checks
	private final Counter balanceChecksCounter;
	// A timer to track balance retrieval duration
	private final Timer balanceRetrievalTimer;

	public BalanceMetric(MeterRegistry registry) {
		this.balanceChecksCounter = Counter.builder("balance.checks").description("Total number of balance checks")
				.register(registry);

		this.balanceRetrievalTimer = Timer.builder("balance.retrieval.time")
				.description("Time taken to retrieve balance").register(registry);
	}

	public void incrementBalanceChecks() {
		balanceChecksCounter.increment();
	}

	public Timer getBalanceRetrievalTimer() {
		return balanceRetrievalTimer;
	}

}
