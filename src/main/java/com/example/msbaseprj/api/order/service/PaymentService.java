package com.example.msbaseprj.api.order.service;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class PaymentService {
    private static final String ORDER_SERVICE_CB = "orderServiceCB";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(PaymentService.class);
    private final Timer paymentTimer;

    public PaymentService(MeterRegistry registry) {
        this.paymentTimer = Timer.builder("payments.latency")
            .description("Payment processing latency")
            .publishPercentiles(0.5, 0.95, 0.99)  // p50, p95, p99
            .publishPercentileHistogram()
            .tag("service", "payments")
            .register(registry);
    }

    @CircuitBreaker(name = ORDER_SERVICE_CB, fallbackMethod = "orderFallback")
    public String processPayment(String orderId) {
       return paymentTimer.record(() -> doProcessPayment(orderId));
    }

    private String doProcessPayment(String orderId) {
        logger.info("Attempting to pay for order: {}", orderId);

        // Simulating remote service call
        if (Math.random() > 0.5) 
            throw new RuntimeException("Order service is down");
        
        return "{\"orderId\":\"" + orderId + "\", \"name\":\"Product X\", \"price\":10.0}";
    }

    public String orderFallback(String orderId, Throwable ex) {
        logger.error("Fallback activated for order: {} due to: {}", orderId, ex.getMessage());

        return "{\"orderId\":\"" + orderId + "\", \"name\":\"Unavailable Product (Fallback)\", \"price\":0.0}";
    }

}
