package com.samlair.trase.agent.web.dto;

/**
 * Response payload for agent resources.
 *
 * @param id agent identifier.
 * @param name agent name.
 * @param description agent description.
 */
public record AgentResponseDto(
		Long id,
		String name,
		String description
) {
}
