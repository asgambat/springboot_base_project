package com.example.msbaseprj.api.secured.balance.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ExternalPaymentServiceHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Simulate a check to an external payment service
        boolean paymentServiceUp = Math.random() > 0.2; // 80% chance it's up

        if (paymentServiceUp) 
            return Health.up().withDetail("ExternalPaymentService", "Available").build();

        return Health.down().withDetail("ExternalPaymentService", "Unavailable").build();
    }

}
