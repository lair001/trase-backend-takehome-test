package com.samlair.trase.agent.rdbms.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for TaskRunAuditEntityUnitTest.
 */
class TaskRunAuditEntityUnitTest {

	/**
	 * Verifies on create sets occurred at when missing.
	 */
	@Test
	void onCreateSetsOccurredAtWhenMissing() {
		TaskRunAuditEntity entity = new TaskRunAuditEntity();
		entity.onCreate();
		assertNotNull(entity.getOccurredAt());
	}

	/**
	 * Verifies on create keeps occurred at when present.
	 */
	@Test
	void onCreateKeepsOccurredAtWhenPresent() {
		TaskRunAuditEntity entity = new TaskRunAuditEntity();
		Instant existing = Instant.parse("2026-01-31T00:00:00Z");
		entity.setOccurredAt(existing);
		entity.onCreate();
		assertEquals(existing, entity.getOccurredAt());
	}
}
