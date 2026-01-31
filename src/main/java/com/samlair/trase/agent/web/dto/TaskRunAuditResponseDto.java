package com.samlair.trase.agent.web.dto;

import com.samlair.trase.agent.domain.enumeration.AuditAction;
import java.time.Instant;

/**
 * Response payload for task run audit records.
 *
 * @param id audit identifier.
 * @param taskRunId task run identifier.
 * @param action audit action.
 * @param status status captured at the time of the action.
 * @param actorUserId authenticated user id.
 * @param actorUsername authenticated username.
 * @param requestId request correlation id.
 * @param occurredAt timestamp of the action.
 */
public record TaskRunAuditResponseDto(
		Long id,
		Long taskRunId,
		AuditAction action,
		String status,
		Long actorUserId,
		String actorUsername,
		String requestId,
		Instant occurredAt
) {
}
