package com.example.msbaseprj.api.order.service;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {

	private final PaymentGatewayClient client;

	public PaymentGatewayHealthIndicator(PaymentGatewayClient client) {
		this.client = client;
	}

	@Override
	public Health health() {
		try {
			boolean reachable = client.ping(); // Simple reachability check
			if (reachable) {
				return Health.up().withDetail("gateway", "Stripe").withDetail("status", "reachable").build();
			}
			return Health.down().withDetail("gateway", "Stripe").withDetail("reason", "Ping failed").build();
		} catch (Exception e) {
			return Health.down(e).withDetail("gateway", "Stripe").build();
		}
	}

}
