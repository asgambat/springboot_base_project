package com.example.msbaseprj.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.msbaseprj.entity.User;

/**
 * Persistence-slice test for {@link UserRepository}. {@code @DataJpaTest} loads
 * only the JPA layer (entities, repositories, an in-memory datasource and a
 * rolled-back transaction per test) instead of the full application context, so
 * the derived query {@code findByEmail} is verified quickly and in isolation.
 * This is the "slice" tier of the testing pyramid, sitting between plain unit
 * tests and full {@code @SpringBootTest} integration tests.
 */
@DataJpaTest
class UserRepositorySliceTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByEmailReturnsPersistedUser() {
		var user = new User();
		user.setName("Slice User");
		user.setEmail("slice-user@example.test");
		entityManager.persistAndFlush(user);

		var found = userRepository.findByEmail("slice-user@example.test");

		assertThat(found).isPresent();
		assertThat(found.get().getName()).isEqualTo("Slice User");
	}

	@Test
	void findByEmailReturnsEmptyForUnknownEmail() {
		assertThat(userRepository.findByEmail("missing@example.test")).isEmpty();
	}
}
