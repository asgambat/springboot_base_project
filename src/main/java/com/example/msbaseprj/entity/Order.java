package com.example.msbaseprj.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
	// @SequenceGenerator(name = "order_seq", sequenceName = "order_seq",
	// allocationSize = 50)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(precision = 14, scale = 2)
	private BigDecimal amount;

	public Order() {
	}

	public Order(BigDecimal amount) {
		this.amount = amount;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

}
