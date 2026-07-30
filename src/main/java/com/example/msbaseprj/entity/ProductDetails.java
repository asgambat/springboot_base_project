package com.example.msbaseprj.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import java.util.Objects;

@Entity
public class ProductDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String manufacturer;
	private String sku;
	private String warranty;

	@OneToOne
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	public ProductDetails() {
	}

	public ProductDetails(String manufacturer, String sku, String warranty) {
		this.manufacturer = manufacturer;
		this.sku = sku;
		this.warranty = warranty;
	}

	public ProductDetails(String manufacturer, String sku, String warranty, Product product) {
		this.manufacturer = manufacturer;
		this.sku = sku;
		this.warranty = warranty;
		this.product = product;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getWarranty() {
		return warranty;
	}

	public void setWarranty(String warranty) {
		this.warranty = warranty;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof ProductDetails))
			return false;

		var productDetails = (ProductDetails) o;
		return Objects.equals(sku, productDetails.sku);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sku);
	}

}
