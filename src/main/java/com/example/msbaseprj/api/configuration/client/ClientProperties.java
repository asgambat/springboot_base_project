package com.example.msbaseprj.api.configuration.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.msbaseprj.api.configuration.client.HttpClientFactory.HttpClientCustomizer;

@ConfigurationProperties(prefix = "app.external-api")
public class ClientProperties {
	private Api api = new Api();
	private Retry retry = new Retry();
	private Connection connection = new Connection();

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

	public Api getApi() {
		return api;
	}

	public void setApi(Api api) {
		this.api = api;
	}

	public static class Connection implements HttpClientCustomizer {
		private long responseTimeoutMillis;
		private long connectionTimeoutMillis;
		private int maxTotalConnections;
		private int maxConnectionsPerRoute;
		private long connectionTtlSeconds;
		private long connectionPoolMaxWaitTimeMillis;
		private int evictionIdleTimeoutSeconds;

		public long getResponseTimeoutMillis() {
			return responseTimeoutMillis;
		}

		public void setResponseTimeoutMillis(long responseTimeoutMillis) {
			this.responseTimeoutMillis = responseTimeoutMillis;
		}

		public long getConnectionTimeoutMillis() {
			return connectionTimeoutMillis;
		}

		public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
			this.connectionTimeoutMillis = connectionTimeoutMillis;
		}

		public int getMaxTotalConnections() {
			return maxTotalConnections;
		}

		public void setMaxTotalConnections(int maxTotalConnections) {
			this.maxTotalConnections = maxTotalConnections;
		}

		public int getMaxConnectionsPerRoute() {
			return maxConnectionsPerRoute;
		}

		public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
			this.maxConnectionsPerRoute = maxConnectionsPerRoute;
		}

		public long getConnectionTtlSeconds() {
			return connectionTtlSeconds;
		}

		public void setConnectionTtlSeconds(long connectionTtlSeconds) {
			this.connectionTtlSeconds = connectionTtlSeconds;
		}

		public long getConnectionPoolMaxWaitTimeMillis() {
			return connectionPoolMaxWaitTimeMillis;
		}

		public void setConnectionPoolMaxWaitTimeMillis(long connectionPoolMaxWaitTimeMillis) {
			this.connectionPoolMaxWaitTimeMillis = connectionPoolMaxWaitTimeMillis;
		}

		public int getEvictionIdleTimeoutSeconds() {
			return evictionIdleTimeoutSeconds;
		}

		public void setEvictionIdleTimeoutSeconds(int evictionIdleTimeoutSeconds) {
			this.evictionIdleTimeoutSeconds = evictionIdleTimeoutSeconds;
		}

	}

	public static class Retry {
		private int maxImmediateAttempts = 2; // additional attempts beyond the first
		private long baseBackoffMs = 300;
		private double multiplier = 2.0;
		private long maxBackoffMs = 5000;

		public int getMaxImmediateAttempts() {
			return this.maxImmediateAttempts;
		}

		public void setMaxImmediateAttempts(int maxImmediateAttempts) {
			this.maxImmediateAttempts = maxImmediateAttempts;
		}

		public long getBaseBackoffMs() {
			return this.baseBackoffMs;
		}

		public void setBaseBackoffMs(long baseBackoffMs) {
			this.baseBackoffMs = baseBackoffMs;
		}

		public double getMultiplier() {
			return this.multiplier;
		}

		public void setMultiplier(double multiplier) {
			this.multiplier = multiplier;
		}

		public long getMaxBackoffMs() {
			return this.maxBackoffMs;
		}

		public void setMaxBackoffMs(long maxBackoffMs) {
			this.maxBackoffMs = maxBackoffMs;
		}
	}

	public static class Api {
		private String baseUrl;
		private String subscriptionKey;
		private boolean enableLoggingInterceptor;

		public boolean isEnableLoggingInterceptor() {
			return enableLoggingInterceptor;
		}

		public void setEnableLoggingInterceptor(boolean enableLoggingInterceptor) {
			this.enableLoggingInterceptor = enableLoggingInterceptor;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getSubscriptionKey() {
			return subscriptionKey;
		}

		public void setSubscriptionKey(String subscriptionKey) {
			this.subscriptionKey = subscriptionKey;
		}

	}

}
