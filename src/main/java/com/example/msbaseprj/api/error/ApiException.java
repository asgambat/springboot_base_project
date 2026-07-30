package com.example.msbaseprj.api.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
	private final ErrorType errorType;
	private final HttpStatus status;

	public ApiException(String message, HttpStatus status, ErrorType errorType, Throwable cause) {

		super(message, cause);

		this.errorType = errorType;
		this.status = status;
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public HttpStatus getStatus() {
		return status;
	}

}
