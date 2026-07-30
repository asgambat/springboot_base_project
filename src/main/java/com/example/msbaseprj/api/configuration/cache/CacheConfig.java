package com.example.msbaseprj.api.configuration.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	@Value("${spring.cache.caffeine.spec}")
	private String caffeineSpec;

	@Bean
	public Caffeine<Object, Object> caffeineConfig() {
		return Caffeine.from(caffeineSpec);
	}

	@Bean
	public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
		var cacheManager = new CaffeineCacheManager();
		cacheManager.setCaffeine(caffeine);
		return cacheManager;
	}

}
