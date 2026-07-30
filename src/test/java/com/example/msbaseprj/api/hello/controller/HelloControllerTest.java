package com.example.msbaseprj.api.hello.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.msbaseprj.api.hello.service.IHelloService;
import com.example.msbaseprj.config.web.WebSecurityConfig;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HelloController.class)
@Import(WebSecurityConfig.class)
class HelloControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IHelloService helloService;

	@Test
	void helloDefaultName() throws Exception {
		when(helloService.hello("World")).thenReturn("Hello, World!");

		mockMvc.perform(get("/api/hello")).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Hello, World!"));
	}

	@Test
	void helloWithName() throws Exception {
		when(helloService.hello("Spring")).thenReturn("Hello, Spring!");

		mockMvc.perform(get("/api/hello").param("name", "Spring")).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Hello, Spring!"));
	}
}
