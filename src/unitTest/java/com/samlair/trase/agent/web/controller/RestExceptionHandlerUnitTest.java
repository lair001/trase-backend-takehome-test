package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.web.dto.ApiErrorDto;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerUnitTest {

	private final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
	private final RestExceptionHandler handler = new RestExceptionHandler(rateLimiterRegistry);

	@Test
	void handleNotFoundReturns404() {
		HttpServletRequest request = mockRequest("/agents/1", "GET");
		ResponseEntity<ApiErrorDto> response = handler.handleNotFound(
				new NotFoundException("missing"), request);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("missing", response.getBody().message());
	}

	@Test
	void handleBadRequestReturns400() {
		HttpServletRequest request = mockRequest("/tasks", "POST");
		ResponseEntity<ApiErrorDto> response = handler.handleBadRequest(
				new BadRequestException("bad"), request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("bad", response.getBody().message());
	}

	@Test
	void handleValidationReturnsFieldErrors() {
		HttpServletRequest request = mockRequest("/tasks", "POST");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "title", "title is required"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		ResponseEntity<ApiErrorDto> response = handler.handleValidation(ex, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Validation failed", response.getBody().message());
		assertEquals(Map.of("title", "title is required"), response.getBody().validationErrors());
	}

	@Test
	void handleRateLimitReturns429() {
		HttpServletRequest request = mockRequest("/agents", "GET");
		RateLimiter limiter = rateLimiterRegistry.rateLimiter("api");
		RequestNotPermitted ex = RequestNotPermitted.createRequestNotPermitted(limiter);

		ResponseEntity<ApiErrorDto> response = handler.handleRateLimit(ex, request);

		assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
		assertEquals("Rate limit exceeded", response.getBody().message());
		assertEquals(String.valueOf(limiter.getRateLimiterConfig().getLimitForPeriod()),
				response.getHeaders().getFirst("X-RateLimit-Limit"));
	}

	@Test
	void handleUnexpectedReturns500() {
		HttpServletRequest request = mockRequest("/tasks", "GET");
		ResponseEntity<ApiErrorDto> response = handler.handleUnexpected(new RuntimeException("boom"), request);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("Unexpected error", response.getBody().message());
		assertNotNull(response.getBody().timestamp());
		assertTrue(response.getBody().status() >= 500);
	}

	private HttpServletRequest mockRequest(String uri, String method) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURI()).thenReturn(uri);
		when(request.getMethod()).thenReturn(method);
		return request;
	}
}
