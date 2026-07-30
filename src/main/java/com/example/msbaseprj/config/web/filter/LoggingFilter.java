package com.example.msbaseprj.config.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoggingFilter implements Filter {
	private final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		var httpRequest = (HttpServletRequest) request;
		var startTime = System.currentTimeMillis();

		logger.info("Request: %s %s".formatted(httpRequest.getMethod(), httpRequest.getRequestURI()));

		// continue
		chain.doFilter(request, response);

		var httpResponse = (HttpServletResponse) response;
		var duration = System.currentTimeMillis() - startTime;
		logger.info("Response: {} {} [{}] in {}ms", httpRequest.getMethod(), httpRequest.getRequestURI(),
				httpResponse.getStatus(), duration);
	}

}
