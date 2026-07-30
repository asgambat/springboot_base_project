package com.example.msbaseprj.api.configuration.client.interceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;

public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {
	private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
	private static final String SENSITIVE_JSON_FIELD_PATTERN = "(?i)(\\\"(?:cardNumber|cvv)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")";

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		logRequest(request, body);

		var response = execution.execute(request, body);

		return logResponse(response);
	}

	private void logRequest(HttpRequest request, byte[] body) {
		log.debug("HTTP REQUEST >> {} {}", request.getMethod(), request.getURI());
		// request.getHeaders().forEach((key, value) -> log.info("HTTP REQUEST HEADER >>
		// {}={}", key, String.join(",", value)));

		if (body.length > 0)
			log.debug("HTTP REQUEST BODY >> {}", maskSensitiveData(new String(body, StandardCharsets.UTF_8)));
	}

	private ClientHttpResponse logResponse(ClientHttpResponse response) throws IOException {
		if (response == null) {
			log.debug("HTTP RESPONSE << null");
			return response;
		}

		log.debug("HTTP RESPONSE << {} {}", response.getStatusCode(), response.getStatusText());
		if (log.isTraceEnabled())
			response.getHeaders()
					.forEach((key, value) -> log.trace("HTTP RESPONSE HEADER << {}={}", key, String.join(",", value)));

		if (log.isDebugEnabled()) {
			try {
				response = new BufferingClientHttpResponseWrapper(response);
				// we read max 10KB of response body, to avoid filling logs with huge responses,
				// but enough for debugging purposes
				var responseBody = new String(response.getBody().readNBytes(10_240), StandardCharsets.UTF_8);
				if (responseBody.isEmpty())
					log.debug("HTTP RESPONSE BODY << EMPTY");
				else
					log.debug("HTTP RESPONSE BODY << {}", maskSensitiveData(responseBody));
			} catch (Exception e) {
				log.trace("Failed to log HTTP response body", e);
			}
		}
		return response;
	}

	static String maskSensitiveData(String body) {
		return body.replaceAll(SENSITIVE_JSON_FIELD_PATTERN, "$1***$2");
	}

	private static final class BufferingClientHttpResponseWrapper implements ClientHttpResponse {
		private final ClientHttpResponse delegate;
		private final byte[] body;

		public BufferingClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
			this.delegate = response;
			this.body = response.getBody().readAllBytes();
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(body);
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return delegate.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return delegate.getStatusText();
		}

		@Override
		public HttpHeaders getHeaders() {
			return delegate.getHeaders();
		}

		@Override
		public void close() {
			delegate.close();
		}

	}

}
