package com.example.msbaseprj.api.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.msbaseprj.api.order.service.OrderEventSerializer;
import com.example.msbaseprj.api.order.service.OrderService;
import com.example.msbaseprj.entity.User;
import com.example.msbaseprj.model.CreateOrderRequest;
import com.example.msbaseprj.repository.OrderRepository;
import com.example.msbaseprj.repository.OutboxEventRepository;
import com.example.msbaseprj.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceRollbackIntegrationTest {
	@Autowired
	private OrderService orderService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@MockitoBean
	private OrderEventSerializer orderEventSerializer;

	private Long userId;

	@BeforeEach
	void setUp() {
		outboxEventRepository.deleteAll();
		orderRepository.deleteAll();
		userRepository.deleteAll();

		var user = new User();
		user.setName("Rollback Test User");
		user.setEmail("rollback-test@example.com");
		userId = userRepository.saveAndFlush(user).getId();
	}

	@Test
	void rollsBackOrderWhenOutboxPayloadCannotBeSerialized() {
		when(orderEventSerializer.serialize(any()))
				.thenThrow(new IllegalStateException("Failed to serialize OrderCreatedEvent"));

		assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(userId, new BigDecimal("19.99"))))
				.isInstanceOf(RuntimeException.class).hasMessage("Failed to serialize OrderCreatedEvent");

		assertThat(orderRepository.count()).isZero();
		assertThat(outboxEventRepository.count()).isZero();
	}
}