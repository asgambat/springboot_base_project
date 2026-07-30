package com.example.msbaseprj.repository;

import java.util.List;
import java.util.Set;

import com.example.msbaseprj.entity.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Product> findProductsByEmailsOr(Set<String> emails) {
		if (emails == null || emails.isEmpty())
			return List.of();

		var cb = entityManager.getCriteriaBuilder();
		var query = cb.createQuery(Product.class);
		Root<Product> product = query.from(Product.class);

		query.select(product).where(createOrPredicates(emails, cb, product));

		return entityManager.createQuery(query).getResultList();
	}

	private Predicate createOrPredicates(Set<String> emails, CriteriaBuilder cb, Root<Product> product) {
		Path<String> emailPath = product.get("email");

		var predicates = emails.stream().map(email -> {
			var pattern = email.contains("%") ? email : "%" + email + "%";
			return cb.like(emailPath, pattern);
		}).toArray(Predicate[]::new);

		return cb.or(predicates);
	}

}
