package com.example.msbaseprj.api.exception;

import java.net.SocketTimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import com.example.msbaseprj.api.error.ErrorType;
import com.example.msbaseprj.api.error.ErrorInfo;
import com.example.msbaseprj.api.model.ApiResult;

import jakarta.validation.ValidationException;

@Component
public class ErrorMapper {

	public <T> ApiResult<T> toResult(Throwable ex) {
		return switch (ex) {
			case RateLimitException e ->
				failure(ErrorType.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", e);
			case ValidationException e -> failure(ErrorType.BAD_REQUEST, HttpStatus.BAD_REQUEST, e.getMessage(), e);
			case ResourceAccessException e when e.getCause() instanceof SocketTimeoutException ->
				failure(ErrorType.TIMEOUT, HttpStatus.GATEWAY_TIMEOUT, "Payment gateway timeout", e);
			case ResourceAccessException e ->
				failure(ErrorType.NETWORK_ERROR, HttpStatus.SERVICE_UNAVAILABLE, "Network error", e);
			default -> failure(ErrorType.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ex);
		};
	}

	private <T> ApiResult<T> failure(ErrorType type, HttpStatus status, String message, Throwable cause) {

		return new ApiResult.Failure<>(new ErrorInfo(message, status, type, cause));
	}

}
