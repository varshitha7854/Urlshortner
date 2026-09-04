package com.example.urlshortener.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.shortener")
public record ShortenerProperties(int codeLength, int defaultExpirationDays) {

	public ShortenerProperties {
		if (codeLength != 7) {
			throw new IllegalArgumentException("codeLength must be 7 for random Base62 short codes");
		}
		if (defaultExpirationDays < 1) {
			throw new IllegalArgumentException("defaultExpirationDays must be at least 1");
		}
	}

	public Duration defaultExpiration() {
		return Duration.ofDays(defaultExpirationDays);
	}
}
