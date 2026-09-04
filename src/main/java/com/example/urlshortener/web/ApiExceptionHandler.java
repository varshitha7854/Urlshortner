package com.example.urlshortener.web;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.urlshortener.dto.ErrorResponse;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlExpiredException;
import com.example.urlshortener.exception.UrlNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidUrlException.class)
	public ResponseEntity<ErrorResponse> invalidUrl(InvalidUrlException ex) {
		return error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> validationError(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream().findFirst()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.orElse("Validation failed");
		return error(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(UrlNotFoundException.class)
	public ResponseEntity<ErrorResponse> notFound(UrlNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(UrlExpiredException.class)
	public ResponseEntity<ErrorResponse> expired(UrlExpiredException ex) {
		return error(HttpStatus.GONE, ex.getMessage());
	}

	private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
		ErrorResponse response = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);
		return ResponseEntity.status(status).body(response);
	}
}
