package com.example.msbaseprj.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msbaseprj.entity.Product;

@DataJpaTest
public class ProductRepositoryIntegrationTest {
	@Autowired
	TestEntityManager entityManager;

	@Autowired
	ProductRepository productRepository;

	@Test
	void givenNewProduct_whenSave_thenSuccess() {
		// given
		var product = new Product("Test Product", "This is a test product", 99.99);

		// when
		var insertedProduct = productRepository.save(product);

		// thens
		assertThat(entityManager.find(Product.class, insertedProduct.getId())).isEqualTo(product);
	}

	@Test
	void givenProductCreated_whenFindById_thenSuccess() {
		// given
		var newProduct = new Product("Test Product", "This is a test product", 99.99);
		entityManager.persist(newProduct);

		// when
		var retrievedProduct = productRepository.findById(newProduct.getId());

		// then
		assertThat(retrievedProduct).contains(newProduct);
	}

	@Test
	void givenProductCreated_whenFindByNameContaining_thenSuccess() {
		var newProduct1 = new Product("Test Product", "This is a test product", 99.99);
		var newProduct2 = new Product("Test Product2", "This is a test product2", 999.99);
		entityManager.persist(newProduct1);
		entityManager.persist(newProduct2);
		var page = org.springframework.data.domain.PageRequest.of(0, 10);
		var products = productRepository.findByNameContaining("Test", page);
		assertThat(products).contains(newProduct1, newProduct2);
	}

	@Test
	void givenProductCreated_whenDelete_thenSuccess() {
		// given
		var newProduct = new Product("Test Product", "This is a test product", 99.99);
		entityManager.persist(newProduct);

		// when
		productRepository.delete(newProduct);

		// then
		assertThat(entityManager.find(Product.class, newProduct.getId())).isNull();
	}

}
