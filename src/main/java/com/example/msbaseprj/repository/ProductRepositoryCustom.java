package com.example.msbaseprj.repository;

import java.util.List;
import java.util.Set;

import com.example.msbaseprj.entity.Product;

public interface ProductRepositoryCustom {
	List<Product> findProductsByEmailsOr(Set<String> emails);

}
