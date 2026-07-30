package com.example.msbaseprj.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import com.example.msbaseprj.dto.UserProjection;
import com.example.msbaseprj.entity.Status;
import com.example.msbaseprj.entity.User;

import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	@Query("""
			    SELECT u FROM User u
			    WHERE u.status = :status AND u.company.id = :companyId
			""")
	List<User> findActiveUsersByCompany(Status status, Long companyId);

	@Query("""
			    SELECT u FROM User u
			    LEFT JOIN FETCH u.orders
			    WHERE u.id = :id
			""")
	Optional<User> findUserWithOrders(Long id);

	@Query("""
			    SELECT DISTINCT u FROM User u
			    LEFT JOIN FETCH u.orders
			""")
	List<User> findAllWithOrders();

	@EntityGraph(attributePaths = {"orders"})
	Optional<User> findWithOrdersById(Long id);

	@EntityGraph(attributePaths = {"orders", "company"})
	@Query("SELECT u FROM User u")
	List<User> findAllWithOrdersAndCompany();

	@QueryHints(value = {@QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100"),
			@QueryHint(name = org.hibernate.jpa.QueryHints.HINT_READONLY, value = "true")})
	@Query("""
			        SELECT u.id   AS userId,
			               u.name AS userName
			        FROM User u
			        where u.status = 'ACTIVE'

			""")
	Stream<UserProjection> findAllBy();

}
