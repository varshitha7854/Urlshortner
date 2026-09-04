package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.urlshortener.config.ShortenerProperties;
import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.repository.UrlMappingRepository;

class UrlShortenerServiceTests {

	private final UrlMappingRepository repository = mock(UrlMappingRepository.class);
	private final Base62CodeGenerator codeGenerator = mock(Base62CodeGenerator.class);
	private final UrlCacheService cacheService = mock(UrlCacheService.class);
	private final ClickTrackingService clickTrackingService = mock(ClickTrackingService.class);
	private final UrlShortenerService service = new UrlShortenerService(repository, codeGenerator, cacheService,
			clickTrackingService, new ShortenerProperties(7, 30));

	@Test
	void shortenCreatesRandomBase62Mapping() {
		when(codeGenerator.randomCode(7)).thenReturn("AbC123x");
		when(repository.existsByShortCode("AbC123x")).thenReturn(false);
		when(repository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
			return invocation.getArgument(0);
		});

		ShortenUrlResponse response = service.shorten("https://example.com/docs", null, "http://localhost:8080");

		assertThat(response.shortCode()).isEqualTo("AbC123x");
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/AbC123x");
		assertThat(response.originalUrl()).isEqualTo("https://example.com/docs");
		assertThat(response.expiresAt()).isAfter(Instant.now().plus(Duration.ofDays(29)));
	}

	@Test
	void shortenRejectsMalformedUrl() {
		assertThatThrownBy(() -> service.shorten("not-a-url", null, "http://localhost:8080"))
			.isInstanceOf(InvalidUrlException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void resolveUsesRedisBeforePostgres() {
		when(cacheService.getOriginalUrl("AbC123x")).thenReturn(Optional.of("https://example.com"));

		String originalUrl = service.resolveOriginalUrl("AbC123x");

		assertThat(originalUrl).isEqualTo("https://example.com");
		verify(repository, never()).findByShortCode(any());
		verify(clickTrackingService).recordClick("AbC123x");
	}
}
