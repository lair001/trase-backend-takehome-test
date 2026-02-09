package com.samlair.trase.agent.rdbms.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for TaskAuditEntityUnitTest.
 */
class TaskAuditEntityUnitTest {

	/**
	 * Verifies on create sets occurred at when missing.
	 */
	@Test
	void onCreateSetsOccurredAtWhenMissing() {
		TaskAuditEntity entity = new TaskAuditEntity();
		entity.onCreate();
		assertNotNull(entity.getOccurredAt());
	}

	/**
	 * Verifies on create keeps occurred at when present.
	 */
	@Test
	void onCreateKeepsOccurredAtWhenPresent() {
		TaskAuditEntity entity = new TaskAuditEntity();
		Instant existing = Instant.parse("2026-01-31T00:00:00Z");
		entity.setOccurredAt(existing);
		entity.onCreate();
		assertEquals(existing, entity.getOccurredAt());
	}
}
