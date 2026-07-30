package com.example.msbaseprj.api.hello.exception;

import java.net.URI;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.example.msbaseprj.api.hello")
public class HelloExceptionHandler {

	@ExceptionHandler(CensuredWordException.class)
	@ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
	public ProblemDetail handleCensuredWordException(CensuredWordException ex, HttpServletRequest request) {
		var problemDetail = ProblemDetail.forStatus(org.springframework.http.HttpStatus.BAD_REQUEST);
		problemDetail.setType(URI.create("https://api.example.com/problems/censured-word"));
		problemDetail.setTitle("Censured Word Detected");
		problemDetail.setDetail(ex.getMessage());
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		return problemDetail;
	}

}
