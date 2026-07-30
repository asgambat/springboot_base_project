package com.example.msbaseprj.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.msbaseprj.api.hello.service.IHelloService;
import com.example.msbaseprj.api.secured.balance.service.BalanceService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IHelloService helloService;

	@MockitoBean
	private BalanceService balanceService;

	@Test
	void runtimeExceptionReturns500WithProblemDetail() throws Exception {
		when(helloService.hello(anyString())).thenThrow(new RuntimeException("Unexpected error"));

		mockMvc.perform(get("/api/hello")).andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Internal Server Error"))
				.andExpect(jsonPath("$.detail").value("Unexpected error"))
				.andExpect(jsonPath("$.type").value("https://api.example.com/problems/global/internal-server-error"))
				.andExpect(jsonPath("$.instance").value("/api/hello"));
	}

	@Test
	void validationErrorReturns400WhenNameIsBlank() throws Exception {
		String body = """
				{
				 "name": "",
				 "email": "test@example.com"
				}
				""";

		mockMvc.perform(post("/api/user").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Global Validation Error"))
				.andExpect(jsonPath("$.type").value("https://api.example.com/problems/global/validation-error"))
				.andExpect(jsonPath("$.instance").value("/api/user"))
				.andExpect(jsonPath("$.validationErrors.name").isArray())
				.andExpect(jsonPath("$.validationErrors.name[0]").isNotEmpty());
	}

	@Test
	void validationErrorReturns400WhenNameIsMissing() throws Exception {
		String body = """
				{
				 "email": "test@example.com"
				}
				""";

		mockMvc.perform(post("/api/user").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.validationErrors.name").isArray());
	}

	@Test
	void noValidationErrorWhenRequestIsValid() throws Exception {
		String body = """
				{
				 "name": "Mario",
				 "email": "MARIO@EXAMPLE.COM"
				}
				""";

		mockMvc.perform(post("/api/user").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Mario"));
	}

}
