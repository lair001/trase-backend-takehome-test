package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.service.AuthService;
import com.samlair.trase.agent.service.TokenRevocationService;
import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

	@Mock
	private AuthService authService;

	@Mock
	private TokenRevocationService tokenRevocationService;

	@InjectMocks
	private AuthController authController;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void loginDelegatesToService() {
		LoginRequestDto request = new LoginRequestDto("user", "pass");
		LoginResponseDto response = new LoginResponseDto("token", "Bearer",
				Instant.parse("2026-01-31T00:00:00Z"), 1L, List.of("ADMIN"));
		when(authService.login(request)).thenReturn(response);

		LoginResponseDto result = authController.login(request);

		assertEquals(response, result);
	}

	@Test
	void logoutRevokesWhenJwtPresent() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("user")
				.claim("jti", "jti-1")
				.expiresAt(Instant.parse("2026-01-31T00:00:00Z"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		ResponseEntity<Void> response = authController.logout();

		verify(tokenRevocationService).revoke("jti-1", jwt.getExpiresAt());
		assertEquals(204, response.getStatusCode().value());
	}

	@Test
	void logoutNoopsWhenNoAuth() {
		ResponseEntity<Void> response = authController.logout();

		verify(tokenRevocationService, never()).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		assertEquals(204, response.getStatusCode().value());
	}

	@Test
	void logoutNoopsWhenAuthNotJwt() {
		SecurityContextHolder.getContext().setAuthentication(null);

		ResponseEntity<Void> response = authController.logout();

		verify(tokenRevocationService, never()).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		assertEquals(204, response.getStatusCode().value());
	}
}
