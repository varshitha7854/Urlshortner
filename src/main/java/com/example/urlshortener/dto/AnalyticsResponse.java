package com.example.urlshortener.dto;

import java.time.Instant;

public record AnalyticsResponse(String shortCode, String originalUrl, Instant createdAt, Instant expiresAt,
		long clickCount, boolean active) {
}
