package com.samlair.trase.agent.rdbms.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for TaskRunEntityUnitTest.
 */
class TaskRunEntityUnitTest {

	/**
	 * Verifies on create sets started at when missing.
	 */
	@Test
	void onCreateSetsStartedAtWhenMissing() {
		TestTaskRunEntity entity = new TestTaskRunEntity();
		assertNull(entity.getStartedAt());

		entity.triggerCreate();

		assertNotNull(entity.getStartedAt());
	}

	/**
	 * Verifies on create does not override started at when present.
	 */
	@Test
	void onCreateDoesNotOverrideStartedAtWhenPresent() {
		TestTaskRunEntity entity = new TestTaskRunEntity();
		Instant started = Instant.parse("2024-01-01T00:00:00Z");
		entity.setStartedAt(started);

		entity.triggerCreate();

		assertEquals(started, entity.getStartedAt());
	}

	private static final class TestTaskRunEntity extends TaskRunEntity {
		/**
		 * Helper for trigger create.
		 */
		void triggerCreate() {
			super.onCreate();
		}
	}
}
