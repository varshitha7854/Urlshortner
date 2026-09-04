package com.example.urlshortener.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.urlshortener.config.CacheProperties;

@Service
public class UrlCacheService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UrlCacheService.class);
	private static final String KEY_PREFIX = "url:";

	private final StringRedisTemplate redisTemplate;
	private final CacheProperties cacheProperties;

	public UrlCacheService(StringRedisTemplate redisTemplate, CacheProperties cacheProperties) {
		this.redisTemplate = redisTemplate;
		this.cacheProperties = cacheProperties;
	}

	public Optional<String> getOriginalUrl(String shortCode) {
		if (!cacheProperties.enabled()) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(redisTemplate.opsForValue().get(key(shortCode)));
		}
		catch (DataAccessException ex) {
			LOGGER.warn("Redis read failed for shortCode={}; falling back to Postgres", shortCode, ex);
			return Optional.empty();
		}
	}

	public void cacheOriginalUrl(String shortCode, String originalUrl, Instant expiresAt) {
		if (!cacheProperties.enabled()) {
			return;
		}
		Duration ttl = ttlUntilExpiry(expiresAt);
		if (ttl.isZero() || ttl.isNegative()) {
			evict(shortCode);
			return;
		}
		try {
			redisTemplate.opsForValue().set(key(shortCode), originalUrl, ttl);
		}
		catch (DataAccessException ex) {
			LOGGER.warn("Redis write failed for shortCode={}; redirect can still use Postgres", shortCode, ex);
		}
	}

	public void evict(String shortCode) {
		if (!cacheProperties.enabled()) {
			return;
		}
		try {
			redisTemplate.delete(key(shortCode));
		}
		catch (DataAccessException ex) {
			LOGGER.warn("Redis eviction failed for shortCode={}", shortCode, ex);
		}
	}

	private Duration ttlUntilExpiry(Instant expiresAt) {
		Duration untilExpiry = Duration.between(Instant.now(), expiresAt);
		if (untilExpiry.compareTo(cacheProperties.urlTtl()) > 0) {
			return cacheProperties.urlTtl();
		}
		return untilExpiry;
	}

	private String key(String shortCode) {
		return KEY_PREFIX + shortCode;
	}
}
