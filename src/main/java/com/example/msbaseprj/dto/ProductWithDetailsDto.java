package com.example.msbaseprj.dto;

public class ProductWithDetailsDto extends ProductDto {
	private final String manufacturer;
	private final String sku;
	private final String warranty;

	public ProductWithDetailsDto(Long id, String name, Double price, String manufacturer, String sku, String warranty) {
		super(id, name, price);
		this.manufacturer = manufacturer;
		this.sku = sku;
		this.warranty = warranty;
	}

	public String getManufacturer() {
		return this.manufacturer;
	}

	public String getSku() {
		return this.sku;
	}

	public String getWarranty() {
		return this.warranty;
	}

}
