package com.example.msbaseprj.api.error;

import org.springframework.http.HttpStatus;

public record ErrorInfo(String message, HttpStatus status, ErrorType type, Throwable cause) {
}
