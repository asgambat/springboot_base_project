package com.example.msbaseprj.api.order.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayClient {

    public boolean ping() {
        // Simulate a ping to the payment gateway
        return Math.random() > 0.2; // 80% chance of being reachable
    }

}
