package com.example.msbaseprj.api.order.service;

import org.springframework.stereotype.Service;

import com.example.msbaseprj.api.order.service.dto.OrderRequest;
import com.example.msbaseprj.api.order.service.dto.OrderResult;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class OrderService {

    private final Counter orderSuccessCounter;
    private final Counter orderFailureCounter;
    private final Timer orderProcessingTimer;
    private final OrderMetrics orderMetrics;

    public OrderService(MeterRegistry registry, OrderMetrics orderMetrics) {
        this.orderMetrics = orderMetrics;
        this.orderSuccessCounter = registry.counter("orders.processed",
            "status", "success");
        this.orderFailureCounter = registry.counter("orders.processed",
            "status", "failure");
        this.orderProcessingTimer = registry.timer("orders.processing.duration");
    }

    public OrderResult processOrder(OrderRequest request) {
        return orderProcessingTimer.record(() -> {
            try {
                OrderResult result = doProcessOrder(request);
                orderMetrics.increment();
                orderSuccessCounter.increment();
                return result;
            } catch (Exception e) {
                orderFailureCounter.increment();
                throw e;
            }
        });
    }

    private OrderResult doProcessOrder(OrderRequest request) {
        return new OrderResult();
    }
    
}