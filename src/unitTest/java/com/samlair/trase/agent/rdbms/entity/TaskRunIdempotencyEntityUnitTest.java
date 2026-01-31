package com.samlair.trase.agent.rdbms.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaskRunIdempotencyEntityUnitTest {

	@Test
	void onCreateSetsTimestampWhenMissing() {
		TaskRunIdempotencyEntity entity = new TaskRunIdempotencyEntity();
		entity.setCreatedAt(null);

		entity.onCreate();

		assertNotNull(entity.getCreatedAt());
	}

	@Test
	void onCreateKeepsTimestampWhenPresent() {
		TaskRunIdempotencyEntity entity = new TaskRunIdempotencyEntity();
		Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
		entity.setCreatedAt(createdAt);

		entity.onCreate();

		assertEquals(createdAt, entity.getCreatedAt());
	}
}
