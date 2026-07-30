package com.example.msbaseprj.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.msbaseprj.dto.ProductDto;
import com.example.msbaseprj.dto.ProductProjection;
import com.example.msbaseprj.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
	List<Product> findByName(String name);
	Page<Product> findByNameContaining(String name, Pageable pageable);
	List<Product> findByPriceGreaterThan(Double price);
	List<Product> findByPriceLessThanEqual(Double price);
	List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
	List<Product> findByNameStartingWith(String prefix);
	List<Product> findByNameEndingWith(String suffix);
	List<Product> findByNameIn(Collection<String> names);
	List<Product> findByNameNotIn(Collection<String> names);
	List<Product> findByNameIgnoreCase(String name);
	List<Product> findByNameOrderByPriceAsc(String name);
	List<Product> findByNameOrderByPriceDesc(String name);
	// String likePattern = "a%b%c";
	List<Product> findByNameLike(String likePattern);
	List<Product> findTop3ByName(String name);
	List<Product> findByNameIsNot(String name);
	List<Product> findByNameIsNull();
	List<Product> findByNameIsNotNull();
	List<Product> findByActiveTrue();
	List<Product> findByActiveFalse();
	List<Product> findByActivationDateBefore(java.time.ZonedDateTime dateTime);
	List<Product> findByActivationDateAfter(java.time.ZonedDateTime dateTime);

	List<Product> findByNameOrDescriptionAndActive(String name, String description, Boolean active);
	List<Product> findByNameOrderByNameDesc(String name);

	@Query("SELECT new com.example.msbaseprj.dto.ProductDto(p.id, p.name, p.price) FROM Product p WHERE p.id = :id")
	ProductDto findProductDtoById(@Param("id") Long id);

	@Query("SELECT p FROM Product p Left JOIN FETCH p.images")
	List<Product> findProductWithImage();

	@Query("""
			        SELECT p FROM Product p
			        LEFT JOIN FETCH p.images
			        WHERE p.id = :id
			""")
	Optional<Product> findByIdWithImages(Long id);

	@Query(value = "SELECT p FROM Product p ORDER BY p.id")
	Page<Product> findAllProductsWithPagination(Pageable pageable);

	@EntityGraph(value = "Product.deep", type = EntityGraph.EntityGraphType.LOAD)
	@Query(value = "SELECT p FROM Product p WHERE p.id = :id")
	Optional<Product> findByIdDeep(@Param("id") Long id);

	@Query("SELECT p FROM Product p WHERE (:cursor IS NULL OR p.id > :cursor) ORDER BY p.id")
	List<Product> findNextPage(@Param("cursor") Long cursor, Pageable pageable);

	@Query(value = "SELECT p FROM Product p WHERE p.name IN :names")
	List<Product> findProductByNameList(@Param("names") Collection<String> names);

	@Query("""
			    SELECT
			            p.id            AS productId,
			            p.name          AS productName,
			            p.price         AS price,

			            d.id            AS detailsId,
			            d.manufacturer  AS manufacturer,

			            c.id            AS categoryId,
			            c.name          AS categoryName
			    FROM Product p
			    LEFT JOIN p.details d
			    LEFT JOIN d.category c
			    WHERE p.id = :id
			""")
	List<ProductProjection> findProductProjectionById(@Param("id") Long id);

	@Modifying
	@Query("update Product p set p.active = :active where p.name = :name")
	int updateProductSetActiveForName(@Param("active") Boolean active, @Param("name") String name);

	@Modifying
	@Query(value = "update Product set active = :active, activation_date = :activationDate where id = :id", nativeQuery = true)
	void updateProduct(@Param("active") Boolean active, @Param("activationDate") java.time.ZonedDateTime activationDate,
			@Param("id") Long id);

}
