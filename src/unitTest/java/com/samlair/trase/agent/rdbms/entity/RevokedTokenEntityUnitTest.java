package com.samlair.trase.agent.rdbms.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RevokedTokenEntityUnitTest {

	@Test
	void onCreateSetsRevokedAtWhenMissing() {
		RevokedTokenEntity entity = new RevokedTokenEntity();
		entity.onCreate();
		assertNotNull(entity.getRevokedAt());
	}

	@Test
	void onCreateKeepsRevokedAtWhenPresent() {
		RevokedTokenEntity entity = new RevokedTokenEntity();
		Instant existing = Instant.parse("2026-01-31T00:00:00Z");
		entity.setRevokedAt(existing);
		entity.onCreate();
		assertEquals(existing, entity.getRevokedAt());
	}
}
