package com.example.urlshortener.exception;

public class UrlExpiredException extends RuntimeException {

	public UrlExpiredException(String shortCode) {
		super("URL mapping has expired for short code: " + shortCode);
	}
}
