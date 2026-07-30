package com.example.msbaseprj.api.exception;

public class RateLimitException extends RuntimeException {
	private final long retryAfterMillis;

	public RateLimitException(String message, long retryAfterMillis) {
		super(message);
		this.retryAfterMillis = retryAfterMillis;
	}

	public long getRetryAfterMillis() {
		return retryAfterMillis;
	}
}
