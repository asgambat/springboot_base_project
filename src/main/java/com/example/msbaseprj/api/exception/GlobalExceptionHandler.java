package com.example.msbaseprj.api.exception;

import static java.util.stream.Collectors.*;

import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.example.msbaseprj.api.order.service.exception.PaymentException;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handle500(Exception ex, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setType(URI.create("https://api.example.com/problems/global/internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_GATEWAY)
    public ProblemDetail handlePayment(PaymentException e, HttpServletRequest request) {
        Sentry.withScope(scope -> {
            scope.setTag("feature", "payments");
            scope.setContexts("request", Map.of(
                "orderId", e.getOrderId(),
                "amount",  e.getAmount()
            ));
            Sentry.captureException(e);
        });
        
        var problemDetail = ProblemDetail.forStatus(org.springframework.http.HttpStatus.BAD_GATEWAY);
        problemDetail.setType(URI.create("https://api.example.com/problems/payments/payment-failed"));
        problemDetail.setTitle("Payment Failed");
        problemDetail.setDetail(e.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
    
    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var problemDetail = getValidationProblemDetail(ex, request);
        problemDetail.setProperty("validationErrors",
             ex.getBindingResult().getFieldErrors().stream()
                 .collect(groupingBy(FieldError::getField, 
                                     mapping(FieldError::getDefaultMessage, toList()))));
        return problemDetail;
    }

    @ExceptionHandler({HandlerMethodValidationException.class})
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        return ex.getBody();
    }

    private ProblemDetail getValidationProblemDetail(Exception ex, HttpServletRequest request) {
        var problemDetail = ProblemDetail.forStatus(org.springframework.http.HttpStatus.BAD_REQUEST);   
        problemDetail.setType(URI.create("https://api.example.com/problems/global/validation-error"));
        problemDetail.setTitle("Global Validation Error");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

}
