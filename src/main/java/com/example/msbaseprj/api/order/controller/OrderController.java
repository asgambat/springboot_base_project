package com.example.msbaseprj.api.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.order.service.OrderService;
import com.example.msbaseprj.model.CreateOrderRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public void createOrder(@Valid @RequestBody CreateOrderRequest request) {
		orderService.createOrder(request);
	}

}
