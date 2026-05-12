package com.example.msbaseprj.api.order.service.exception;

public class PaymentException extends RuntimeException {
    private final long orderId;
    private final String amount;

    public PaymentException(long orderId, String amount, String message) {
        super(message);
        this.orderId = orderId;
        this.amount = amount;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getAmount() {
        return amount;
    }

}
