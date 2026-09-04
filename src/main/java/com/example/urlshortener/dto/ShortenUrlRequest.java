package com.example.urlshortener.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShortenUrlRequest(
		@NotBlank(message = "originalUrl is required") @Size(max = 2048, message = "originalUrl must be 2048 characters or fewer") String originalUrl,
		Instant expiresAt) {
}
