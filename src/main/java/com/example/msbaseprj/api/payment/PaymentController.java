package com.example.msbaseprj.api.payment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.payment.PaymentService.PaymentRequest;
import com.example.msbaseprj.api.payment.PaymentService.PaymentResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/payments")
@Validated
public class PaymentController {
	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	public PaymentResponse createPayment(
			@RequestHeader("Idempotency-Key") @NotBlank @Size(max = 255) String idempotencyKey,
			@Valid @RequestBody PaymentRequest request) {
		return paymentService.processPayment(request, idempotencyKey).getOrThrow();
	}

}
