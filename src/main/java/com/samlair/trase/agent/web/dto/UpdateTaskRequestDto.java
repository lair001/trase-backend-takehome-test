package com.samlair.trase.agent.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Request payload to update a task.
 *
 * @param title updated task title.
 * @param description updated task description.
 * @param supportedAgentIds agents that can run the task.
 * @param supportedAgentId single agent that can run the task.
 */
public record UpdateTaskRequestDto(
		@NotBlank(message = "title is required")
		@Size(max = 200, message = "title must be 200 characters or fewer")
		String title,
		@NotBlank(message = "description is required")
		@Size(max = 2000, message = "description must be 2000 characters or fewer")
		String description,
		@JsonAlias({"supported_agent_ids"})
		Set<Long> supportedAgentIds,
		@JsonAlias({"supported_agent_id"})
		Long supportedAgentId
) {
	@AssertTrue(message = "supportedAgentIds or supportedAgentId must contain at least one agent id")
	boolean isSupportedAgentValid() {
		return (supportedAgentIds != null && !supportedAgentIds.isEmpty()) || supportedAgentId != null;
	}
}
