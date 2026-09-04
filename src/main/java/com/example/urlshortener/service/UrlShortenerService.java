package com.example.urlshortener.service;

import java.net.URI;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.urlshortener.config.ShortenerProperties;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.dto.AnalyticsResponse;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlExpiredException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.repository.UrlMappingRepository;

@Service
public class UrlShortenerService {

	private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

	private final UrlMappingRepository repository;
	private final Base62CodeGenerator codeGenerator;
	private final UrlCacheService cacheService;
	private final ClickTrackingService clickTrackingService;
	private final ShortenerProperties shortenerProperties;

	public UrlShortenerService(UrlMappingRepository repository, Base62CodeGenerator codeGenerator,
			UrlCacheService cacheService, ClickTrackingService clickTrackingService,
			ShortenerProperties shortenerProperties) {
		this.repository = repository;
		this.codeGenerator = codeGenerator;
		this.cacheService = cacheService;
		this.clickTrackingService = clickTrackingService;
		this.shortenerProperties = shortenerProperties;
	}

	public ShortenUrlResponse shorten(String originalUrl, Instant requestedExpiry, String baseUrl) {
		String normalizedUrl = normalizeAndValidate(originalUrl);
		Instant expiresAt = requestedExpiry == null ? Instant.now().plus(shortenerProperties.defaultExpiration())
				: requestedExpiry;
		if (!expiresAt.isAfter(Instant.now())) {
			throw new InvalidUrlException("expiresAt must be in the future");
		}

		UrlMapping saved = createWithUniqueCode(normalizedUrl, expiresAt);
		String shortUrl = baseUrl.replaceAll("/+$", "") + "/" + saved.getShortCode();
		return new ShortenUrlResponse(saved.getShortCode(), shortUrl, saved.getOriginalUrl(), saved.getCreatedAt(),
				saved.getExpiresAt());
	}

	public String resolveOriginalUrl(String shortCode) {
		validateShortCode(shortCode);
		return cacheService.getOriginalUrl(shortCode)
			.map(originalUrl -> {
				clickTrackingService.recordClick(shortCode);
				return originalUrl;
			})
			.orElseGet(() -> resolveFromPostgres(shortCode));
	}

	public AnalyticsResponse analytics(String shortCode) {
		validateShortCode(shortCode);
		UrlMapping mapping = repository.findByShortCode(shortCode).orElseThrow(() -> new UrlNotFoundException(shortCode));
		return new AnalyticsResponse(mapping.getShortCode(), mapping.getOriginalUrl(), mapping.getCreatedAt(),
				mapping.getExpiresAt(), mapping.getClickCount(), mapping.getExpiresAt().isAfter(Instant.now()));
	}

	private UrlMapping createWithUniqueCode(String originalUrl, Instant expiresAt) {
		for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
			String shortCode = codeGenerator.randomCode(shortenerProperties.codeLength());
			if (repository.existsByShortCode(shortCode)) {
				continue;
			}
			try {
				return repository.save(new UrlMapping(shortCode, originalUrl, expiresAt));
			}
			catch (DataIntegrityViolationException ex) {
				if (attempt == MAX_CODE_GENERATION_ATTEMPTS - 1) {
					throw ex;
				}
			}
		}
		throw new IllegalStateException("Could not generate a unique short code");
	}

	private String resolveFromPostgres(String shortCode) {
		UrlMapping mapping = repository.findByShortCode(shortCode).orElseThrow(() -> new UrlNotFoundException(shortCode));
		if (!mapping.getExpiresAt().isAfter(Instant.now())) {
			cacheService.evict(shortCode);
			throw new UrlExpiredException(shortCode);
		}
		cacheService.cacheOriginalUrl(shortCode, mapping.getOriginalUrl(), mapping.getExpiresAt());
		clickTrackingService.recordClick(shortCode);
		return mapping.getOriginalUrl();
	}

	private String normalizeAndValidate(String url) {
		try {
			URI uri = URI.create(url.trim()).normalize();
			String scheme = uri.getScheme();
			if (scheme == null || uri.getHost() == null
					|| (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
				throw new InvalidUrlException("Only absolute http(s) URLs are supported");
			}
			return uri.toString();
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidUrlException("Malformed URL");
		}
	}

	private void validateShortCode(String shortCode) {
		if (shortCode == null || shortCode.length() != shortenerProperties.codeLength()
				|| !shortCode.matches("[0-9A-Za-z]+")) {
			throw new UrlNotFoundException(shortCode);
		}
	}
}
