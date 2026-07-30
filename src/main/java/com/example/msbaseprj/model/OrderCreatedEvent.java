package com.example.msbaseprj.model;

import java.math.BigDecimal;

import com.example.msbaseprj.entity.Order;

public record OrderCreatedEvent(Long id, Long userId, BigDecimal amount) {
	public OrderCreatedEvent(Order order) {
		this(order.getId(), order.getUser().getId(), order.getAmount());
	}

}
