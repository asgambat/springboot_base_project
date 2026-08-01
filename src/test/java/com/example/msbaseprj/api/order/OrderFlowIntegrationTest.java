package com.example.msbaseprj.api.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.msbaseprj.entity.User;
import com.example.msbaseprj.repository.OrderRepository;
import com.example.msbaseprj.repository.OutboxEventRepository;
import com.example.msbaseprj.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	private Long userId;

	@BeforeEach
	void setUp() {
		outboxEventRepository.deleteAll();
		orderRepository.deleteAll();
		userRepository.deleteAll();

		var user = new User();
		user.setName("Order Test User");
		user.setEmail("order-test@example.com");
		userId = userRepository.saveAndFlush(user).getId();
	}

	@Test
	void createsOrderAndOutboxEventAtomically() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"userId\":%d,\"amount\":19.99}".formatted(userId))).andExpect(status().isOk());

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(outboxEventRepository.count()).isEqualTo(1);
		var event = outboxEventRepository.findAll().getFirst();
		assertThat(event.getAggregateType()).isEqualTo("Order");
		assertThat(event.getEventType()).isEqualTo("OrderCreated");
		assertThat(event.getPayload()).contains("\"amount\":19.99");
	}

	@Test
	void populatesAuditingFieldsWhenOrderIsCreated() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"userId\":%d,\"amount\":19.99}".formatted(userId))).andExpect(status().isOk());

		var order = orderRepository.findAll().getFirst();
		assertThat(order.getCreatedAt()).isNotNull();
		assertThat(order.getUpdatedAt()).isNotNull();
		assertThat(order.getCreatedBy()).isEqualTo("system");
		assertThat(order.getLastModifiedBy()).isEqualTo("system");
	}

	@Test
	void rejectsInvalidOrderBeforePersistingAnything() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"userId\":%d,\"amount\":-0.01}".formatted(userId))).andExpect(status().isBadRequest());

		assertThat(orderRepository.count()).isZero();
		assertThat(outboxEventRepository.count()).isZero();
	}
}