package com.example.urlshortener.exception;

public class UrlNotFoundException extends RuntimeException {

	public UrlNotFoundException(String shortCode) {
		super("No URL mapping found for short code: " + shortCode);
	}
}
