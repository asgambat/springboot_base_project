package com.example.msbaseprj.api.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.msbaseprj.repository.PaymentIdempotencyRepository;
import com.example.msbaseprj.api.payment.PaymentService.PaymentRequest;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowIntegrationTest {
	private static final AtomicInteger gatewayRequestCount = new AtomicInteger();
	private static final AtomicReference<String> lastIdempotencyKey = new AtomicReference<>();
	private static final ConcurrentLinkedQueue<String> receivedIdempotencyKeys = new ConcurrentLinkedQueue<>();
	private static final AtomicReference<GatewayResponse> gatewayResponse = new AtomicReference<>(
			GatewayResponse.success());
	private static final AtomicReference<GatewayResponse> gatewayResponseAfterFirstRequest = new AtomicReference<>();
	private static final AtomicReference<CountDownLatch> gatewayRequestObserved = new AtomicReference<>();
	private static final ExecutorService gatewayExecutor = Executors.newCachedThreadPool();
	private static final HttpServer gatewayServer = startGateway();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PaymentIdempotencyRepository paymentIdempotencyRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private BulkheadRegistry bulkheadRegistry;

	@Autowired
	private PaymentChargeGatewayClient paymentGatewayClient;

	@DynamicPropertySource
	static void configureGateway(DynamicPropertyRegistry registry) {
		registry.add("app.external-api.api.base-url", () -> "http://localhost:" + gatewayServer.getAddress().getPort());
		registry.add("app.external-api.api.enable-logging-interceptor", () -> false);
		registry.add("app.external-api.connection.response-timeout-millis", () -> 1000);
		registry.add("app.external-api.connection.connection-timeout-millis", () -> 250);
		registry.add("app.external-api.connection.connection-pool-max-wait-time-millis", () -> 250);
		registry.add("resilience4j.circuitbreaker.instances.paymentGateway.sliding-window-size", () -> 2);
		registry.add("resilience4j.circuitbreaker.instances.paymentGateway.minimum-number-of-calls", () -> 2);
		registry.add("resilience4j.circuitbreaker.instances.paymentGateway.failure-rate-threshold", () -> 100);
		registry.add("resilience4j.bulkhead.instances.paymentGateway.maxConcurrentCalls", () -> 1);
	}

	@BeforeEach
	void resetGateway() {
		paymentIdempotencyRepository.deleteAll();
		circuitBreakerRegistry.circuitBreaker("paymentGateway").reset();
		gatewayRequestCount.set(0);
		lastIdempotencyKey.set(null);
		receivedIdempotencyKeys.clear();
		gatewayResponse.set(GatewayResponse.success());
		gatewayResponseAfterFirstRequest.set(null);
		gatewayRequestObserved.set(null);
	}

	@AfterAll
	static void stopGateway() {
		gatewayServer.stop(0);
		gatewayExecutor.shutdownNow();
	}

	@Test
	void rejectsUnauthenticatedPaymentRequests() throws Exception {
		mockMvc.perform(post("/payments").header("Idempotency-Key", "payment-auth")
				.contentType(MediaType.APPLICATION_JSON).content(paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isUnauthorized());

		assertThat(gatewayRequestCount).hasValue(0);
	}

	@Test
	void validatesRequestBeforeCallingGateway() throws Exception {
		mockMvc.perform(authenticatedPayment("payment-validation", paymentRequest("0.00", "XYZ", "12")))
				.andExpect(status().isBadRequest());

		assertThat(gatewayRequestCount).hasValue(0);
	}

	@Test
	void mapsRateLimitAndForwardsRetryAfter() throws Exception {
		gatewayResponse.set(new GatewayResponse(429, 0, "3", "{}"));

		mockMvc.perform(authenticatedPayment("payment-rate-limit", paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "3"))
				.andExpect(jsonPath("$.errorType").value("RATE_LIMIT"));
	}

	@Test
	void mapsGatewayServerErrorsToInternalServerError() throws Exception {
		gatewayResponse.set(new GatewayResponse(503, 0, null, "{}"));

		mockMvc.perform(authenticatedPayment("payment-5xx", paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isInternalServerError()).andExpect(jsonPath("$.errorType").value("SERVER_ERROR"));
	}

	@Test
	void mapsGatewayTimeoutToGatewayTimeout() throws Exception {
		gatewayResponse.set(new GatewayResponse(200, 1500, null,
				"{\"transactionId\":\"tx-timeout\",\"status\":\"APPROVED\",\"message\":\"ok\"}"));

		mockMvc.perform(authenticatedPayment("payment-timeout", paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.errorType").value("TIMEOUT"));
	}

	@Test
	void replaysCompletedPaymentWithoutCallingGatewayAgain() throws Exception {
		var request = paymentRequest("10.00", "EUR", "123");

		mockMvc.perform(authenticatedPayment("payment-replay", request)).andExpect(status().isOk())
				.andExpect(jsonPath("$.transactionId").value("tx-123"));
		mockMvc.perform(authenticatedPayment("payment-replay", request)).andExpect(status().isOk())
				.andExpect(jsonPath("$.transactionId").value("tx-123"));

		assertThat(gatewayRequestCount).hasValue(1);
		assertThat(lastIdempotencyKey).hasValue("payment-replay");
	}

	@Test
	void replaysCompletedPaymentWhenOnlyCvvChanges() throws Exception {
		mockMvc.perform(authenticatedPayment("payment-cvv-not-persisted", paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isOk());
		mockMvc.perform(authenticatedPayment("payment-cvv-not-persisted", paymentRequest("10.00", "EUR", "999")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.transactionId").value("tx-123"));

		assertThat(gatewayRequestCount).hasValue(1);
	}

	@Test
	void reclaimsExpiredPendingPaymentAndCallsGatewayAgain() throws Exception {
		var idempotencyKey = "payment-expired-lease";
		var request = paymentRequest("10.00", "EUR", "123");
		gatewayResponse.set(new GatewayResponse(503, 0, null, "{}"));

		mockMvc.perform(authenticatedPayment(idempotencyKey, request)).andExpect(status().isInternalServerError());
		circuitBreakerRegistry.circuitBreaker("paymentGateway").reset();

		jdbcTemplate.update("UPDATE payment_idempotency SET processing_lease_until = ? WHERE idempotency_key = ?",
				java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)), idempotencyKey);
		gatewayResponse.set(GatewayResponse.success());

		mockMvc.perform(authenticatedPayment(idempotencyKey, request)).andExpect(status().isOk())
				.andExpect(jsonPath("$.transactionId").value("tx-123"));

		assertThat(gatewayRequestCount).hasValue(3);
	}

	@Test
	void retriesTransientGatewayFailureWithTheSameIdempotencyKey() throws Exception {
		var idempotencyKey = "payment-retry";
		gatewayResponse.set(new GatewayResponse(503, 0, null, "{}"));
		gatewayResponseAfterFirstRequest.set(GatewayResponse.success());

		mockMvc.perform(authenticatedPayment(idempotencyKey, paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.transactionId").value("tx-123"));

		assertThat(gatewayRequestCount).hasValue(2);
		assertThat(receivedIdempotencyKeys).containsExactly(idempotencyKey, idempotencyKey);
	}

	@Test
	void opensCircuitBreakerAndBlocksSubsequentPaymentBeforeCallingGateway() throws Exception {
		gatewayResponse.set(new GatewayResponse(503, 0, null, "{}"));

		for (var attempt = 0; attempt < 3 && circuitBreakerRegistry.circuitBreaker("paymentGateway")
				.getState() != CircuitBreaker.State.OPEN; attempt++) {
			mockMvc.perform(authenticatedPayment("payment-circuit-" + attempt, paymentRequest("10.00", "EUR", "123")))
					.andExpect(status().isInternalServerError());
		}

		assertThat(circuitBreakerRegistry.circuitBreaker("paymentGateway").getState())
				.isEqualTo(CircuitBreaker.State.OPEN);
		var requestsBeforeBlockedPayment = gatewayRequestCount.get();

		mockMvc.perform(authenticatedPayment("payment-circuit-blocked", paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isInternalServerError());

		assertThat(gatewayRequestCount).hasValue(requestsBeforeBlockedPayment);
	}

	@Test
	void rejectsConcurrentPaymentWhenBulkheadIsSaturated() throws Exception {
		gatewayResponse.set(new GatewayResponse(200, 225, null,
				"{\"transactionId\":\"tx-delayed\",\"status\":\"APPROVED\",\"message\":\"ok\"}"));
		var requestObserved = new CountDownLatch(1);
		gatewayRequestObserved.set(requestObserved);
		assertThat(bulkheadRegistry.bulkhead("paymentGateway").getBulkheadConfig().getMaxConcurrentCalls())
				.isEqualTo(1);
		var clientExecutor = Executors.newSingleThreadExecutor();
		try {
			var firstPayment = clientExecutor
					.submit(() -> paymentGatewayClient.charge(gatewayRequest(), "payment-bulkhead-first"));
			assertThat(requestObserved.await(1, TimeUnit.SECONDS)).isTrue();

			assertThatThrownBy(() -> paymentGatewayClient.charge(gatewayRequest(), "payment-bulkhead-second"))
					.isInstanceOf(io.github.resilience4j.bulkhead.BulkheadFullException.class);

			assertThat(firstPayment.get(1, TimeUnit.SECONDS).transactionId()).isEqualTo("tx-delayed");
			assertThat(gatewayRequestCount).hasValue(1);
		} finally {
			clientExecutor.shutdownNow();
		}
	}

	@Test
	void rejectsIdempotencyKeyReusedWithDifferentPayment() throws Exception {
		mockMvc.perform(authenticatedPayment("payment-conflict", paymentRequest("10.00", "EUR", "123")))
				.andExpect(status().isOk());
		mockMvc.perform(authenticatedPayment("payment-conflict", paymentRequest("11.00", "EUR", "123")))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.errorType").value("CONFLICT"));

		assertThat(gatewayRequestCount).hasValue(1);
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedPayment(
			String idempotencyKey, String request) {
		return post("/payments").with(httpBasic("user", "password")).header("Idempotency-Key", idempotencyKey)
				.contentType(MediaType.APPLICATION_JSON).content(request);
	}

	private static String paymentRequest(String amount, String currency, String cvv) {
		return """
				{
				  "cardNumber": "4111111111111111",
				  "cardHolder": "Mario Rossi",
				  "expirationDate": "12/30",
				  "cvv": "%s",
				  "amount": %s,
				  "currency": "%s"
				}
				""".formatted(cvv, amount, currency);
	}

	private static PaymentRequest gatewayRequest() {
		return new PaymentRequest("4111111111111111", "Mario Rossi", "12/30", "123", new BigDecimal("10.00"), "EUR");
	}

	private static HttpServer startGateway() {
		try {
			var server = HttpServer.create(new InetSocketAddress(0), 0);
			server.setExecutor(gatewayExecutor);
			server.createContext("/v1/charges", exchange -> {
				var requestNumber = gatewayRequestCount.incrementAndGet();
				var requestObserved = gatewayRequestObserved.get();
				if (requestObserved != null) {
					requestObserved.countDown();
				}
				lastIdempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
				receivedIdempotencyKeys.add(lastIdempotencyKey.get());
				var responseAfterFirstRequest = gatewayResponseAfterFirstRequest.get();
				var response = requestNumber == 1 || responseAfterFirstRequest == null
						? gatewayResponse.get()
						: responseAfterFirstRequest;
				try {
					if (response.delayMillis() > 0) {
						Thread.sleep(response.delayMillis());
					}
					if (response.retryAfter() != null) {
						exchange.getResponseHeaders().add("Retry-After", response.retryAfter());
					}
					var responseBody = response.body().getBytes(StandardCharsets.UTF_8);
					exchange.getResponseHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
					exchange.sendResponseHeaders(response.status(), responseBody.length);
					exchange.getResponseBody().write(responseBody);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				} catch (IOException ignored) {
					// The client may have timed out before the test gateway writes its response.
				} finally {
					exchange.close();
				}
			});
			server.start();
			return server;
		} catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	private record GatewayResponse(int status, long delayMillis, String retryAfter, String body) {
		private static GatewayResponse success() {
			return new GatewayResponse(200, 0, null,
					"{\"transactionId\":\"tx-123\",\"status\":\"APPROVED\",\"message\":\"ok\"}");
		}
	}
}