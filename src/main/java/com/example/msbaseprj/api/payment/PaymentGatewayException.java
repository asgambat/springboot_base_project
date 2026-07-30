package com.example.msbaseprj.api.payment;

public class PaymentGatewayException extends RuntimeException {
	public PaymentGatewayException(String message) {
		super(message);
	}
}