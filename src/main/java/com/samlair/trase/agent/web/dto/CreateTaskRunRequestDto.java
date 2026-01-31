package com.samlair.trase.agent.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload to start a task run.
 *
 * @param taskId task identifier.
 * @param agentId agent identifier.
 */
public record CreateTaskRunRequestDto(
		@NotNull(message = "taskId is required")
		@Positive(message = "taskId must be positive")
		Long taskId,
		@NotNull(message = "agentId is required")
		@Positive(message = "agentId must be positive")
		Long agentId
) {
}
