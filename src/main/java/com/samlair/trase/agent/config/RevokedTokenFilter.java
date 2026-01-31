package com.samlair.trase.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samlair.trase.agent.service.TokenRevocationService;
import com.samlair.trase.agent.web.dto.ApiErrorDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests using revoked JWTs.
 */
@Component
@RequiredArgsConstructor
public class RevokedTokenFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RevokedTokenFilter.class);

	private final TokenRevocationService tokenRevocationService;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			Jwt jwt = token.getToken();
			String jti = jwt.getId();
			if (jti != null && tokenRevocationService.isRevoked(jti)) {
				log.info("Rejected revoked token for {} {}", request.getMethod(), request.getRequestURI());
				writeUnauthorized(response, request.getRequestURI());
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private void writeUnauthorized(HttpServletResponse response, String path) throws IOException {
		ApiErrorDto error = new ApiErrorDto(
				Instant.now(),
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				"Token revoked",
				path,
				null
		);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(error));
	}
}
