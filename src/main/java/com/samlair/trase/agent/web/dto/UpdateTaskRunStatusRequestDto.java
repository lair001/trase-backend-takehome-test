package com.samlair.trase.agent.web.dto;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to update a task run status.
 *
 * @param status new status for the task run.
 */
public record UpdateTaskRunStatusRequestDto(
		@NotNull(message = "status is required")
		TaskRunStatus status
) {
}
