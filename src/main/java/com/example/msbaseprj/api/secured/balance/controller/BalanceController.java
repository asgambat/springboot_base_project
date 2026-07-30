package com.example.msbaseprj.api.secured.balance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.secured.balance.service.BalanceService;
import com.example.msbaseprj.aspect.Auditable;

@RestController
@RequestMapping("/api/secured")
public class BalanceController {
	private final BalanceService balanceService;

	public BalanceController(BalanceService balanceService) {
		this.balanceService = balanceService;
	}

	@GetMapping("/balance")
	@Auditable(action = "Check balance")
	public String balance() {
		return balanceService.getBalance();
	}

}
