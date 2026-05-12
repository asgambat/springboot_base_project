package com.example.msbaseprj.api.exception;

public class TooManyRequestException extends RuntimeException {
    private final long retryAfterMillis;

    public TooManyRequestException(String message, long retryAfterMillis) {
        super(message);
        this.retryAfterMillis = retryAfterMillis;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }

}
