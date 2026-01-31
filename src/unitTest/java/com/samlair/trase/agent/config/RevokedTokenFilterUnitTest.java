package com.samlair.trase.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samlair.trase.agent.service.TokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokedTokenFilterUnitTest {

	@Mock
	private TokenRevocationService tokenRevocationService;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private FilterChain filterChain;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@InjectMocks
	private RevokedTokenFilter filter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void allowsWhenNotJwt() throws Exception {
		filter.doFilterInternal(request, response, filterChain);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void allowsWhenNoJti() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("user")
				.expiresAt(Instant.parse("2026-01-31T00:00:00Z"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(tokenRevocationService, never()).isRevoked(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void allowsWhenNotRevoked() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("user")
				.claim("jti", "jti-1")
				.expiresAt(Instant.parse("2026-01-31T00:00:00Z"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
		when(tokenRevocationService.isRevoked("jti-1")).thenReturn(false);

		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
	}

	@Test
	void rejectsWhenRevoked() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("user")
				.claim("jti", "jti-2")
				.expiresAt(Instant.parse("2026-01-31T00:00:00Z"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
		when(tokenRevocationService.isRevoked("jti-2")).thenReturn(true);
		when(request.getMethod()).thenReturn("GET");
		when(request.getRequestURI()).thenReturn("/agents");
		StringWriter body = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(body));
		when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
				.thenReturn("{\"message\":\"Token revoked\"}");

		filter.doFilterInternal(request, response, filterChain);

		verify(filterChain, never()).doFilter(request, response);
		verify(response).setStatus(401);
		assertTrue(body.toString().contains("Token revoked"));
	}
}
