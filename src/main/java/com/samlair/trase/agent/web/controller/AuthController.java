package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.service.AuthService;
import com.samlair.trase.agent.service.TokenRevocationService;
import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final TokenRevocationService tokenRevocationService;

	/**
	 * Exchanges credentials for a JWT.
	 *
	 * @param request login request
	 * @return JWT response
	 */
	@PostMapping("/login")
	public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
		return authService.login(request);
	}

	/**
	 * Revokes the currently authenticated JWT.
	 *
	 * @return 204 when revoked (or if no token is present)
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			Jwt jwt = token.getToken();
			tokenRevocationService.revoke(jwt.getId(), jwt.getExpiresAt());
		}
		return ResponseEntity.noContent().build();
	}
}
