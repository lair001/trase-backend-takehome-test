package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.domain.exception.UnauthorizedException;
import com.samlair.trase.agent.web.dto.ApiErrorDto;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps exceptions into consistent API error responses.
 */
@RestControllerAdvice
public class RestExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
	private final RateLimiterRegistry rateLimiterRegistry;

	public RestExceptionHandler(RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiErrorDto> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		log.info("Not found: {} {}", request.getMethod(), request.getRequestURI());
		return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorDto> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		log.info("Bad request: {} {}", request.getMethod(), request.getRequestURI());
		return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiErrorDto> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
		log.info("Unauthorized: {} {}", request.getMethod(), request.getRequestURI());
		return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		log.info("Validation failed: {} {} {}", request.getMethod(), request.getRequestURI(), errors.keySet());
		return buildError(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
	}

	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<ApiErrorDto> handleRateLimit(RequestNotPermitted ex, HttpServletRequest request) {
		log.warn("Rate limit exceeded: {} {}", request.getMethod(), request.getRequestURI());
		ResponseEntity<ApiErrorDto> response = buildError(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", request, null);
		return withRateLimitHeaders(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorDto> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, null);
	}

	private ResponseEntity<ApiErrorDto> buildError(HttpStatus status, String message, HttpServletRequest request,
			Map<String, String> validationErrors) {
		ApiErrorDto apiError = new ApiErrorDto(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				validationErrors
		);
		return ResponseEntity.status(status).body(apiError);
	}

	private ResponseEntity<ApiErrorDto> withRateLimitHeaders(ResponseEntity<ApiErrorDto> response) {
		RateLimiter limiter = rateLimiterRegistry.rateLimiter("api");
		long limit = limiter.getRateLimiterConfig().getLimitForPeriod();
		long remaining = limiter.getMetrics().getAvailablePermissions();
		long resetSeconds = Math.max(0L, limiter.getRateLimiterConfig().getLimitRefreshPeriod().toSeconds());
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-RateLimit-Limit", String.valueOf(limit));
		headers.add("X-RateLimit-Remaining", String.valueOf(Math.max(0L, remaining)));
		headers.add("X-RateLimit-Reset", String.valueOf(resetSeconds));
		return ResponseEntity.status(response.getStatusCode()).headers(headers).body(response.getBody());
	}
}
