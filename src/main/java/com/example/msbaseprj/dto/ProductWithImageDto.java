package com.example.msbaseprj.dto;

import java.util.Set;

public class ProductWithImageDto extends ProductDto {
	private final Set<String> imageUrls;

	public ProductWithImageDto(Long id, String name, Double price, Set<String> imageUrls) {
		super(id, name, price);
		this.imageUrls = Set.copyOf(imageUrls);
	}

	public Set<String> getImageUrls() {
		return this.imageUrls;
	}

}
