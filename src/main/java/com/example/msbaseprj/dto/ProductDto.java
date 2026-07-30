package com.example.msbaseprj.dto;

public class ProductDto {
	private final Long id;
	private final String name;
	private final Double price;

	public ProductDto(Long id, String name, Double price) {
		this.id = id;
		this.name = name;
		this.price = price;
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Double getPrice() {
		return this.price;
	}

}
