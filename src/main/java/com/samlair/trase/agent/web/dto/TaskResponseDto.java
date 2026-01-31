package com.samlair.trase.agent.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

/**
 * Response payload for task resources.
 *
 * @param id task identifier.
 * @param title task title.
 * @param description task description.
 * @param supportedAgentIds agents that can run the task.
 * @param supportedAgentId single supported agent id when only one agent is attached.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskResponseDto(
		Long id,
		String title,
		String description,
		Set<Long> supportedAgentIds,
		Long supportedAgentId
) {
}
