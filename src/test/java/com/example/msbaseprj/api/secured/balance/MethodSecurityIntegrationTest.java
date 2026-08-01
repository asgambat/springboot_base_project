package com.example.msbaseprj.api.secured.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.msbaseprj.api.secured.balance.service.BalanceService;

/**
 * Verifies that {@code @EnableMethodSecurity} enforces the
 * {@code @PreAuthorize} rule on the service method regardless of the entry
 * point, not only via URL-based rules.
 */
@SpringBootTest
class MethodSecurityIntegrationTest {

	@Autowired
	private BalanceService balanceService;

	@Test
	void deniesUnauthenticatedInvocation() {
		assertThatThrownBy(() -> balanceService.getBalance()).isInstanceOf(AuthenticationException.class);
	}

	@Test
	@WithMockUser
	void allowsAuthenticatedInvocation() {
		assertThat(balanceService.getBalance()).contains("balance");
	}
}
