package com.example.msbaseprj.api.secured.balance.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {
	private final BalanceMetric balanceMetric;

	public BalanceService(BalanceMetric balanceMetric) {
		this.balanceMetric = balanceMetric;
	}

	@PreAuthorize("isAuthenticated()")
	public String getBalance() {
		balanceMetric.getBalanceRetrievalTimer().record(() -> {
			try {
				Thread.sleep((long) (Math.random() * 500)); // just simulating some processing time
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		balanceMetric.incrementBalanceChecks();

		return "Your balance is: " + Math.round(Math.random() * 10000) + " USD";

	}

}
