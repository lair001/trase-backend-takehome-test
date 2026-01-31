package com.samlair.trase.agent.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to update an agent.
 *
 * @param name updated agent name.
 * @param description updated agent description.
 */
public record UpdateAgentRequestDto(
		@NotBlank(message = "name is required")
		@Size(max = 200, message = "name must be 200 characters or fewer")
		String name,
		@NotBlank(message = "description is required")
		@Size(max = 2000, message = "description must be 2000 characters or fewer")
		String description
) {
}
