package com.example.urlshortener.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.urlshortener.dto.AnalyticsResponse;
import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.service.UrlShortenerService;

import jakarta.validation.Valid;

@RestController
public class UrlController {

	private final UrlShortenerService urlShortenerService;

	public UrlController(UrlShortenerService urlShortenerService) {
		this.urlShortenerService = urlShortenerService;
	}

	@PostMapping("/api/shorten")
	public ResponseEntity<ShortenUrlResponse> shorten(@Valid @RequestBody ShortenUrlRequest request) {
		String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		ShortenUrlResponse response = urlShortenerService.shorten(request.originalUrl(), request.expiresAt(), baseUrl);
		return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		String originalUrl = urlShortenerService.resolveOriginalUrl(shortCode);
		return ResponseEntity.status(302).location(URI.create(originalUrl)).build();
	}

	@GetMapping("/api/analytics/{shortCode}")
	public AnalyticsResponse analytics(@PathVariable String shortCode) {
		return urlShortenerService.analytics(shortCode);
	}
}
