package com.example.msbaseprj.entity;

import java.time.Instant;

import com.example.msbaseprj.api.payment.PaymentService.PaymentResponse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_idempotency")
public class PaymentIdempotency {
	@Id
	@Column(name = "idempotency_key", length = 255)
	private String idempotencyKey;

	@Column(name = "request_fingerprint", nullable = false, length = 64)
	private String requestFingerprint;

	@Column(name = "transaction_id", length = 255)
	private String transactionId;

	@Column(name = "payment_status", length = 100)
	private String paymentStatus;

	@Column(name = "response_message", length = 1000)
	private String responseMessage;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "processing_lease_until", nullable = false)
	private Instant processingLeaseUntil;

	protected PaymentIdempotency() {
	}

	public PaymentIdempotency(String idempotencyKey, String requestFingerprint, Instant processingLeaseUntil) {
		this.idempotencyKey = idempotencyKey;
		this.requestFingerprint = requestFingerprint;
		this.processingLeaseUntil = processingLeaseUntil;
	}

	public String getRequestFingerprint() {
		return requestFingerprint;
	}

	public boolean isCompleted() {
		return completedAt != null;
	}

	public void complete(PaymentResponse response) {
		transactionId = response.transactionId();
		paymentStatus = response.status();
		responseMessage = response.message();
		completedAt = Instant.now();
		processingLeaseUntil = completedAt;
	}

	public PaymentResponse toPaymentResponse() {
		return new PaymentResponse(transactionId, paymentStatus, responseMessage);
	}
}