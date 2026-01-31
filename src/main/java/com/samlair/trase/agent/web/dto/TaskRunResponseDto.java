package com.samlair.trase.agent.web.dto;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import java.time.Instant;

/**
 * Response payload for task run resources.
 *
 * @param id task run identifier.
 * @param taskId task identifier.
 * @param agentId agent identifier.
 * @param status current run status.
 * @param startedAt start timestamp.
 * @param completedAt completion timestamp.
 */
public record TaskRunResponseDto(
		Long id,
		Long taskId,
		Long agentId,
		TaskRunStatus status,
		Instant startedAt,
		Instant completedAt
) {
}
