package com.example.msbaseprj.api.order.service;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OrderMetrics {

    private final Counter processedOrders;

    public OrderMetrics(MeterRegistry registry) {
        this.processedOrders = Counter.builder("orders.processed")
                .description("Total orders successfully processed")
                .register(registry);
    }

    public void increment() {
        processedOrders.increment();
    }
    
}
