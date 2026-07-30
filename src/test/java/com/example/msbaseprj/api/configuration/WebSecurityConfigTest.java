package com.example.msbaseprj.api.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.msbaseprj.api.hello.service.IHelloService;
import com.example.msbaseprj.api.secured.balance.service.BalanceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private IHelloService helloService;

	@MockitoBean
	private BalanceService balanceService;

	@Test
	void publicEndpointAccessibleWithoutAuthentication() throws Exception {
		when(helloService.hello("World")).thenReturn("Hello, World!");

		mockMvc.perform(get("/api/hello")).andExpect(status().isOk());
	}

	@Test
	void securedEndpointReturns401WithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/secured/balance")).andExpect(status().isUnauthorized());
	}

	@Test
	void securedEndpointAccessibleWithValidCredentials() throws Exception {
		when(balanceService.getBalance()).thenReturn("Your balance is: 500 USD");

		mockMvc.perform(get("/api/secured/balance").with(httpBasic("user", "password"))).andExpect(status().isOk());
	}

	@Test
	void securedEndpointReturns401WithInvalidCredentials() throws Exception {
		mockMvc.perform(get("/api/secured/balance").with(httpBasic("user", "wrongpassword")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void securedEndpointReturns401WithUnknownUser() throws Exception {
		mockMvc.perform(get("/api/secured/balance").with(httpBasic("unknown", "password")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void passwordEncoderIsBCrypt() {
		assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
	}

	@Test
	void passwordEncoderMatchesRawPassword() {
		String raw = "password";
		String encoded = passwordEncoder.encode(raw);

		assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
	}

	@Test
	void passwordEncoderDoesNotMatchWrongPassword() {
		String encoded = passwordEncoder.encode("password");

		assertThat(passwordEncoder.matches("wrongpassword", encoded)).isFalse();
	}

}
