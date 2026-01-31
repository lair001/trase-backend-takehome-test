package com.samlair.trase.agent.rdbms.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditedEntityUnitTest {

	@Test
	void onCreateSetsCreatedAndUpdated() {
		TestAuditedEntity entity = new TestAuditedEntity();
		assertNull(entity.getCreatedAt());
		assertNull(entity.getUpdatedAt());

		entity.triggerCreate();

		assertNotNull(entity.getCreatedAt());
		assertNotNull(entity.getUpdatedAt());
	}

	@Test
	void onUpdateRefreshesUpdatedAt() {
		TestAuditedEntity entity = new TestAuditedEntity();
		entity.setUpdatedAt(Instant.EPOCH);

		entity.triggerUpdate();

		assertNotEquals(Instant.EPOCH, entity.getUpdatedAt());
	}

	private static final class TestAuditedEntity extends AuditedEntity {
		void triggerCreate() {
			super.onCreate();
		}

		void triggerUpdate() {
			super.onUpdate();
		}
	}
}
