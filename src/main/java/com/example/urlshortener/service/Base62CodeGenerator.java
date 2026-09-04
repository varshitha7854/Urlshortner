package com.example.urlshortener.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class Base62CodeGenerator {

	private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

	private final SecureRandom secureRandom = new SecureRandom();

	public String randomCode(int length) {
		StringBuilder code = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			code.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
		}
		return code.toString();
	}
}

