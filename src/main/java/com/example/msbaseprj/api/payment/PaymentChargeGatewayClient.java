package com.example.msbaseprj.api.payment;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.msbaseprj.api.payment.PaymentService.PaymentRequest;
import com.example.msbaseprj.api.payment.PaymentService.PaymentResponse;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.ValidationException;

@Component
public class PaymentChargeGatewayClient {
	private final RestClient restClient;

	public PaymentChargeGatewayClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Retry(name = "paymentGateway")
	@CircuitBreaker(name = "paymentGateway")
	@Bulkhead(name = "paymentGateway", type = Bulkhead.Type.SEMAPHORE)
	public PaymentResponse charge(PaymentRequest request, String idempotencyKey) {
		return restClient.post().uri("/v1/charges").header("Idempotency-Key", idempotencyKey).body(request).retrieve()
				.onStatus(status -> status.is4xxClientError() && status.value() != 429,
						(clientRequest, clientResponse) -> {
							throw new ValidationException("Invalid request");
						})
				.onStatus(HttpStatusCode::is5xxServerError, (clientRequest, clientResponse) -> {
					throw new PaymentGatewayException("Gateway error");
				}).body(PaymentResponse.class);
	}
}