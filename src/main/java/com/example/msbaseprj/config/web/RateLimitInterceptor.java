package com.example.msbaseprj.config.web;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Server-side, in-memory request rate limiter based on the token-bucket
 * algorithm (Bucket4j). One bucket is kept per client key (first
 * {@code X-Forwarded-For} hop, or the remote address). When a client exhausts
 * its tokens the request is rejected with {@code 429 Too Many Requests}, an RFC
 * 7807 {@link ProblemDetail} body and a {@code Retry-After} header.
 *
 * <p>
 * The 429 response is written directly here instead of throwing, because
 * exceptions raised from an interceptor do not reliably flow through
 * {@code @RestControllerAdvice}.
 */
public class RateLimitInterceptor implements HandlerInterceptor {
	private final RateLimitProperties properties;
	private final ObjectMapper objectMapper;
	private final Counter rejectedCounter;
	private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

	public RateLimitInterceptor(RateLimitProperties properties, ObjectMapper objectMapper,
			MeterRegistry meterRegistry) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.rejectedCounter = Counter.builder("http.rate_limit")
				.description("Number of HTTP requests rejected by the rate limiter").tag("outcome", "rejected")
				.register(meterRegistry);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		if (!properties.isEnabled()) {
			return true;
		}

		var bucket = buckets.computeIfAbsent(clientKey(request), key -> newBucket());
		var probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));
			return true;
		}

		rejectRequest(request, response, probe);
		return false;
	}

	private void rejectRequest(HttpServletRequest request, HttpServletResponse response, ConsumptionProbe probe)
			throws IOException {
		var retryAfterSeconds = Math.max(1, (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0));
		rejectedCounter.increment();

		var problemDetail = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
		problemDetail.setType(URI.create("https://api.example.com/problems/global/rate-limit-exceeded"));
		problemDetail.setTitle("Too Many Requests");
		problemDetail.setDetail("Rate limit exceeded. Please retry later.");
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("retryAfterSeconds", retryAfterSeconds);

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
		response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
	}

	private Bucket newBucket() {
		Bandwidth limit = Bandwidth.builder().capacity(properties.getCapacity())
				.refillGreedy(properties.getCapacity(), properties.getRefillPeriod()).build();
		return Bucket.builder().addLimit(limit).build();
	}

	private String clientKey(HttpServletRequest request) {
		var forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
