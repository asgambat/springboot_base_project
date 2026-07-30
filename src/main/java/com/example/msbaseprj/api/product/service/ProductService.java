package com.example.msbaseprj.api.product.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.msbaseprj.api.exception.NotFoundException;
import com.example.msbaseprj.dto.CursorPageResponse;
import com.example.msbaseprj.dto.ProductDto;
import com.example.msbaseprj.dto.ProductProjection;
import com.example.msbaseprj.dto.ProductWithImageDto;
import com.example.msbaseprj.entity.Product;
import com.example.msbaseprj.entity.ProductDetails;
import com.example.msbaseprj.repository.ProductRepository;

@Service
public class ProductService {
	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Page<Product> getAllProducts(int pageNo, int pageSize) {
		var pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.findAll(pageable);
	}

	public Page<Product> searchProducts(String name, int pageNo, int pageSize) {
		var pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.findByNameContaining(name, pageable);
	}

	public CursorPageResponse<Product> getProductsCursorPagination(Long cursor, int pageSize) {
		var products = getProducts(cursor, pageSize);

		// Check if there are more records
		boolean hasNext = products.size() > pageSize;

		// Remove the extra record if it exists
		if (hasNext)
			products = products.subList(0, pageSize);

		return new CursorPageResponse<>(products, pageSize, getNextCursor(products), hasNext);
	}

	public Optional<ProductWithImageDto> getProductWithImages(Long id) {
		return productRepository.findByIdWithImages(id).map(product -> new ProductWithImageDto(product.getId(),
				product.getName(), product.getPrice(), productImages(product)));
	}

	public ProductProjection getProductProjectionById(Long id) {
		var result = productRepository.findProductProjectionById(id);
		if (result.isEmpty())
			throw new NotFoundException("Product not found with id: " + id);

		return result.get(0);
	}

	private Set<String> productImages(Product product) {
		return product.getImages().stream().map(image -> image.getUrl()).collect(Collectors.toSet());
	}

	private Long getNextCursor(List<Product> products) {
		// Get the next cursor (last product's ID)
		Long nextCursor = null;
		if (products.isEmpty() == false)
			nextCursor = products.get(products.size() - 1).getId();
		return nextCursor;
	}

	private List<Product> getProducts(Long cursor, int pageSize) {
		// Create pageable with pageSize + 1 to check if more records exist
		var pageable = PageRequest.of(0, pageSize + 1);

		var products = productRepository.findNextPage(cursor, pageable);
		return products;
	}

	public Page<Product> getAllProductsSorted(int pageNo, int pageSize, String sortBy, String sortDirection) {
		var sort = createSort(sortBy, sortDirection);
		var pageable = PageRequest.of(pageNo, pageSize, sort);

		return productRepository.findAll(pageable);
	}

	private Sort createSort(String sortBy, String sortDirection) {
		var direction = Sort.Direction.ASC;
		if ("desc".equalsIgnoreCase(sortDirection))
			direction = Sort.Direction.DESC;

		return Sort.by(direction, sortBy);
	}

	public void createProduct(ProductDto product) {
		var newProduct = new Product(product.getName(), "", product.getPrice());
		var productDetails = new ProductDetails("manufacturer", "sku", "warranty", newProduct);
		newProduct.setDetails(productDetails);
		productRepository.save(newProduct);
	}

	public Product getProductWithDetails(Long id) {
		return productRepository.findByIdDeep(id)
				.orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
	}

}
