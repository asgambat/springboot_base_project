package com.example.msbaseprj.api.order.service;

import org.springframework.stereotype.Component;

import com.example.msbaseprj.model.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderEventSerializer {
	private final ObjectMapper objectMapper;

	public OrderEventSerializer(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String serialize(OrderCreatedEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize OrderCreatedEvent", ex);
		}
	}
}