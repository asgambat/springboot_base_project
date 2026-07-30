package com.example.msbaseprj.api.configuration.client;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.classic.HttpClient;

public class HttpClientFactory {
	public interface HttpClientCustomizer {
		long getResponseTimeoutMillis();
		long getConnectionTimeoutMillis();
		int getMaxTotalConnections();
		int getMaxConnectionsPerRoute();
		long getConnectionTtlSeconds();
		long getConnectionPoolMaxWaitTimeMillis();
		int getEvictionIdleTimeoutSeconds();
	}

	public static HttpClient create(HttpClientCustomizer customizer) {
		var evictionIdleTimeSec = customizer.getEvictionIdleTimeoutSeconds(); // idle time after which connections in
																				// the pool can be evicted

		return HttpClients.custom().setDefaultRequestConfig(createRequestConfig(customizer))
				.setConnectionManager(createConnectionManager(customizer)).evictExpiredConnections()
				.evictIdleConnections(Timeout.ofSeconds(evictionIdleTimeSec)).disableAutomaticRetries() // we handle
																										// retries at
																										// the service
																										// level with
																										// Spring Retry,
																										// so we disable
																										// the default
																										// HttpClient
																										// retry
																										// mechanism to
																										// avoid
																										// unexpected
																										// interactions
																										// between the
																										// two
				.build();
	}

	private static RequestConfig createRequestConfig(HttpClientCustomizer customizer) {
		var connRequestTimeoutMillis = customizer.getConnectionPoolMaxWaitTimeMillis(); // max time to wait for a
																						// connection from the pool
		var responseTimeoutMillis = customizer.getResponseTimeoutMillis(); // response timeout aka read timeout

		return RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofMilliseconds(connRequestTimeoutMillis))
				.setResponseTimeout(Timeout.ofMilliseconds(responseTimeoutMillis)).build();
	}

	private static HttpClientConnectionManager createConnectionManager(HttpClientCustomizer customizer) {
		var result = new PoolingHttpClientConnectionManager();
		result.setDefaultConnectionConfig(createConnectionConfig(customizer));
		result.setMaxTotal(customizer.getMaxTotalConnections());
		result.setDefaultMaxPerRoute(customizer.getMaxConnectionsPerRoute());
		return result;
	}

	private static ConnectionConfig createConnectionConfig(HttpClientCustomizer customizer) {
		var connectTimeoutMillis = customizer.getConnectionTimeoutMillis(); // max wait time to establish a connection
		var connTtlSec = customizer.getConnectionTtlSeconds(); // time to live for connections in the pool

		return ConnectionConfig.custom().setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMillis))
				.setTimeToLive(Timeout.ofSeconds(connTtlSec)).build();
	}
}
