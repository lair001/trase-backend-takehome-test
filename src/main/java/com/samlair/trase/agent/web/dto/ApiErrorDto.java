package com.samlair.trase.agent.web.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Error response payload for API failures.
 *
 * @param timestamp time of the error.
 * @param status HTTP status code.
 * @param error status reason.
 * @param message human-readable error message.
 * @param path request path.
 * @param validationErrors validation field errors keyed by field name.
 */
public record ApiErrorDto(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, String> validationErrors
) {
}
