package com.example.msbaseprj.api.greeting.model;

/**
 * Version 2 of the greeting representation. It evolves {@link GreetingV1}
 * additively by exposing the resolved language and the served API version,
 * without changing the meaning of the existing {@code message} field.
 */
public record GreetingV2(String message, String language, String apiVersion) {
}
