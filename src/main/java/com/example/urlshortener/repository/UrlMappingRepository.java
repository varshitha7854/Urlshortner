package com.example.urlshortener.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.urlshortener.domain.UrlMapping;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

	Optional<UrlMapping> findByShortCode(String shortCode);

	boolean existsByShortCode(String shortCode);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update UrlMapping u set u.clickCount = u.clickCount + 1 where u.shortCode = :shortCode")
	int incrementClickCount(@Param("shortCode") String shortCode);
}
