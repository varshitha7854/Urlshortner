package com.example.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.urlshortener.repository.UrlMappingRepository;

@Service
public class ClickTrackingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClickTrackingService.class);

	private final UrlMappingRepository repository;

	public ClickTrackingService(UrlMappingRepository repository) {
		this.repository = repository;
	}

	@Async
	@Transactional
	public void recordClick(String shortCode) {
		try {
			repository.incrementClickCount(shortCode);
		}
		catch (RuntimeException ex) {
			LOGGER.warn("Failed to increment click count for shortCode={}", shortCode, ex);
		}
	}
}
