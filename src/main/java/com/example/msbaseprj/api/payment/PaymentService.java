package com.example.msbaseprj.api.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.msbaseprj.api.exception.ErrorMapper;
import com.example.msbaseprj.api.error.ErrorInfo;
import com.example.msbaseprj.api.error.ErrorType;
import com.example.msbaseprj.api.model.ApiResult;
import com.example.msbaseprj.api.payment.validation.Iso4217Currency;
import com.example.msbaseprj.entity.PaymentIdempotency;
import com.example.msbaseprj.repository.PaymentIdempotencyRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.ValidationException;

@Service
public class PaymentService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentService.class);
	private final PaymentChargeGatewayClient paymentGatewayClient;
	private final ErrorMapper errorMapper;
	private final PaymentIdempotencyRepository paymentIdempotencyRepository;
	private final Duration processingLeaseDuration;
	private final Counter replayCounter;
	private final Counter conflictCounter;
	private final Counter reclaimCounter;

	public PaymentService(PaymentChargeGatewayClient paymentGatewayClient, ErrorMapper errorMapper,
			PaymentIdempotencyRepository paymentIdempotencyRepository,
			@Value("${app.payment.idempotency-processing-lease:PT5M}") Duration processingLeaseDuration,
			MeterRegistry meterRegistry) {
		this.paymentGatewayClient = paymentGatewayClient;
		this.errorMapper = errorMapper;
		this.paymentIdempotencyRepository = paymentIdempotencyRepository;
		this.processingLeaseDuration = processingLeaseDuration;
		this.replayCounter = meterRegistry.counter("payments.idempotency", "outcome", "replay");
		this.conflictCounter = meterRegistry.counter("payments.idempotency", "outcome", "conflict");
		this.reclaimCounter = meterRegistry.counter("payments.idempotency", "outcome", "reclaimed");
	}

	public ApiResult<PaymentResponse> processPayment(PaymentRequest request, String idempotencyKey) {
		var requestFingerprint = fingerprint(request);
		var existing = paymentIdempotencyRepository.findById(idempotencyKey);
		if (existing.isPresent()) {
			return resolveExisting(existing.get(), request, requestFingerprint, idempotencyKey);
		}

		var payment = new PaymentIdempotency(idempotencyKey, requestFingerprint, leaseUntil(Instant.now()));
		try {
			paymentIdempotencyRepository.saveAndFlush(payment);
		} catch (DataIntegrityViolationException ex) {
			return paymentIdempotencyRepository.findById(idempotencyKey).map(
					existingPayment -> resolveExisting(existingPayment, request, requestFingerprint, idempotencyKey))
					.orElseGet(() -> failure(ErrorType.CONFLICT, "Payment request is already being processed", ex));
		}

		return callGatewayAndComplete(request, idempotencyKey, payment);
	}

	private ApiResult<PaymentResponse> callGatewayAndComplete(PaymentRequest request, String idempotencyKey,
			PaymentIdempotency payment) {

		try {
			var response = paymentGatewayClient.charge(request, idempotencyKey);

			payment.complete(response);
			paymentIdempotencyRepository.save(payment);
			return new ApiResult.Success<>(response);
		} catch (Exception ex) {
			log.error("Payment processing failed", ex);
			return errorMapper.toResult(ex);
		}
	}

	private ApiResult<PaymentResponse> resolveExisting(PaymentIdempotency existing, PaymentRequest request,
			String requestFingerprint, String idempotencyKey) {
		if (!existing.getRequestFingerprint().equals(requestFingerprint)) {
			conflictCounter.increment();
			return failure(ErrorType.CONFLICT, "Idempotency key was already used with a different payment request",
					null);
		}
		if (existing.isCompleted()) {
			replayCounter.increment();
			return new ApiResult.Success<>(existing.toPaymentResponse());
		}

		var now = Instant.now();
		if (paymentIdempotencyRepository.reclaimExpiredLease(idempotencyKey, requestFingerprint, now,
				leaseUntil(now)) == 1) {
			reclaimCounter.increment();
			return callGatewayAndComplete(request, idempotencyKey,
					new PaymentIdempotency(idempotencyKey, requestFingerprint, leaseUntil(now)));
		}
		return failure(ErrorType.CONFLICT, "Payment request is already being processed", null);
	}

	private Instant leaseUntil(Instant now) {
		return now.plus(processingLeaseDuration);
	}

	private ApiResult<PaymentResponse> failure(ErrorType errorType, String message, Throwable cause) {
		return new ApiResult.Failure<>(
				new ErrorInfo(message, org.springframework.http.HttpStatus.CONFLICT, errorType, cause));
	}

	private String fingerprint(PaymentRequest request) {
		var canonicalRequest = String.join("|", request.cardNumber(), request.cardHolder(), request.expirationDate(),
				request.amount().stripTrailingZeros().toPlainString(), request.currency());
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			return java.util.HexFormat.of().formatHex(digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	public record PaymentRequest(@NotBlank @Pattern(regexp = "\\d{12,19}") String cardNumber,
			@NotBlank @Pattern(regexp = "[A-Za-z .'-]{2,100}") String cardHolder,
			@NotBlank @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}") String expirationDate,
			@NotBlank @Pattern(regexp = "\\d{3,4}") String cvv,
			@NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
			@NotBlank @Iso4217Currency String currency) {
	}

	public record PaymentResponse(String transactionId, String status, String message) {
	}

}