package com.samlair.trase.agent.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Login response payload containing the JWT and metadata.
 */
public record LoginResponseDto(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		Long userId,
		List<String> roles
) {
}
