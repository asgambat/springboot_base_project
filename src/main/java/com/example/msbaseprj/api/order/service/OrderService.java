package com.example.msbaseprj.api.order.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.msbaseprj.api.order.service.dto.OrderRequest;
import com.example.msbaseprj.api.order.service.dto.OrderResult;
import com.example.msbaseprj.entity.Order;
import com.example.msbaseprj.entity.OutboxEvent;
import com.example.msbaseprj.entity.User;
import com.example.msbaseprj.model.CreateOrderRequest;
import com.example.msbaseprj.model.OrderCreatedEvent;
import com.example.msbaseprj.repository.OrderRepository;
import com.example.msbaseprj.repository.OutboxEventRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class OrderService {
	private final Counter orderSuccessCounter;
	private final Counter orderFailureCounter;
	private final Timer orderProcessingTimer;
	private final OrderMetrics orderMetrics;

	private final OrderRepository orderRepository;
	private final OutboxEventRepository outboxRepository;
	private final OrderEventSerializer orderEventSerializer;

	@PersistenceContext
	private EntityManager entityManager;

	public OrderService(MeterRegistry registry, OrderMetrics orderMetrics, OrderRepository orderRepository,
			OutboxEventRepository outboxRepository, OrderEventSerializer orderEventSerializer) {
		this.orderMetrics = orderMetrics;
		this.orderRepository = orderRepository;
		this.outboxRepository = outboxRepository;
		this.orderEventSerializer = orderEventSerializer;
		this.orderSuccessCounter = registry.counter("orders.processed", "status", "success");
		this.orderFailureCounter = registry.counter("orders.processed", "status", "failure");
		this.orderProcessingTimer = registry.timer("orders.processing.duration");
	}

	@Transactional(rollbackFor = Exception.class, timeout = 5)
	public Order createOrder(CreateOrderRequest request) {
		var newOrder = new Order(request.amount());
		newOrder.setUser(entityManager.getReference(User.class, request.userId()));
		var order = orderRepository.save(newOrder);

		var event = new OutboxEvent();
		event.setAggregateType("Order");
		event.setAggregateId(order.getId().toString());
		event.setEventType("OrderCreated");
		event.setPayload(orderEventSerializer.serialize(new OrderCreatedEvent(order)));
		event.setCreatedAt(LocalDateTime.now());

		outboxRepository.save(event);
		return order;
	}

	public OrderResult processOrder(OrderRequest request) {
		return orderProcessingTimer.record(() -> {
			try {
				var result = doProcessOrder(request);
				orderMetrics.increment();
				orderSuccessCounter.increment();
				return result;
			} catch (Exception e) {
				orderFailureCounter.increment();
				throw e;
			}
		});
	}

	private OrderResult doProcessOrder(OrderRequest request) {
		return new OrderResult();
	}

}