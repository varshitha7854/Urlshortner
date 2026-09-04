package com.example.urlshortener.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(boolean enabled, Duration urlTtl) {

	public CacheProperties {
		if (urlTtl == null || urlTtl.isNegative() || urlTtl.isZero()) {
			throw new IllegalArgumentException("urlTtl must be positive");
		}
	}
}
