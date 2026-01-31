package com.samlair.trase.agent.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload for JWT issuance.
 */
public record LoginRequestDto(
		@NotBlank String username,
		@NotBlank String password
) {
}
