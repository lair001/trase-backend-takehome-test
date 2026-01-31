package com.samlair.trase.agent.web.dto;

import com.samlair.trase.agent.domain.enumeration.AuditAction;
import java.time.Instant;

/**
 * Response payload for task audit records.
 *
 * @param id audit identifier.
 * @param taskId task identifier.
 * @param action audit action.
 * @param actorUserId authenticated user id.
 * @param actorUsername authenticated username.
 * @param requestId request correlation id.
 * @param occurredAt timestamp of the action.
 */
public record TaskAuditResponseDto(
		Long id,
		Long taskId,
		AuditAction action,
		Long actorUserId,
		String actorUsername,
		String requestId,
		Instant occurredAt
) {
}
