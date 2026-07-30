package com.example.msbaseprj.dto;

public interface ProductProjection {

	Long getProductId();
	String getProductName();
	Double getPrice();

	Long getDetailsId();
	String getManufacturer();

	Long getCategoryId();
	String getCategoryName();

}
