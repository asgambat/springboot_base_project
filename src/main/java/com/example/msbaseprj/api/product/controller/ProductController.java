package com.example.msbaseprj.api.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.example.msbaseprj.api.product.service.ProductService;
import com.example.msbaseprj.dto.CursorPageResponse;
import com.example.msbaseprj.dto.ProductDto;
import com.example.msbaseprj.dto.ProductProjection;
import com.example.msbaseprj.dto.ProductWithDetailsDto;
import com.example.msbaseprj.entity.Product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	// Example: GET /api/products?pageNo=0&pageSize=10
	public Map<String, Object> getAllProducts(@Min(0) @RequestParam(defaultValue = "0") int pageNo,
			@Min(1) @Max(100) @RequestParam(defaultValue = "10") int pageSize) {
		var page = productService.getAllProducts(pageNo, pageSize);
		return createResponseAsMap(page);
	}

	@GetMapping("/search")
	// Example: GET /api/products/search?name=phone&pageNo=0&pageSize=10
	public Map<String, Object> searchProducts(@NotBlank @RequestParam String name,
			@Min(0) @RequestParam(defaultValue = "0") int pageNo,
			@Min(1) @Max(100) @RequestParam(defaultValue = "10") int pageSize) {
		var page = productService.searchProducts(name, pageNo, pageSize);
		return createResponseAsMap(page);
	}

	@GetMapping("/sorted")
	public Map<String, Object> getAllProducts(@Min(0) @RequestParam(defaultValue = "0") int pageNo,
			@Min(1) @Max(100) @RequestParam(defaultValue = "10") int pageSize,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDirection) {
		var page = productService.getAllProductsSorted(pageNo, pageSize, sortBy, sortDirection);
		return createResponseAsMap(page);
	}

	@GetMapping("/cursor")
	public CursorPageResponse<Product> getProductsCursor(@RequestParam(required = false) Long cursor,
			@Min(1) @Max(100) @RequestParam(defaultValue = "10") int pageSize) {
		return productService.getProductsCursorPagination(cursor, pageSize);
	}

	@PostMapping
	@ResponseStatus(code = org.springframework.http.HttpStatus.CREATED)
	public void createProduct(@RequestBody ProductDto product) {
		productService.createProduct(product);
	}

	@GetMapping("/{id}/details")
	public ProductWithDetailsDto getProductWithDetails(@PathVariable Long id) {
		var product = productService.getProductWithDetails(id);
		var details = product.getDetails();
		return new ProductWithDetailsDto(product.getId(), product.getName(), product.getPrice(),
				details.getManufacturer(), details.getSku(), details.getWarranty());
	}

	@GetMapping("/{id}/projection")
	public ProductProjection getProductProjectionById(@PathVariable Long id) {
		return productService.getProductProjectionById(id);
	}

	private Map<String, Object> createResponseAsMap(Page<Product> page) {
		return Map.of("products", page.getContent(), "currentPage", page.getNumber(), "totalItems",
				page.getTotalElements(), "totalPages", page.getTotalPages(), "hasNext", page.hasNext(), "hasPrevious",
				page.hasPrevious());
	}

}
