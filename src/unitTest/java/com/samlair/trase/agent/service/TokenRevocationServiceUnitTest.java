package com.samlair.trase.agent.service;

import com.samlair.trase.agent.rdbms.dao.RevokedTokenDao;
import com.samlair.trase.agent.rdbms.entity.RevokedTokenEntity;
import com.samlair.trase.agent.service.impl.TokenRevocationServiceImpl;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceUnitTest {

	@Mock
	private RevokedTokenDao revokedTokenDao;

	@InjectMocks
	private TokenRevocationServiceImpl service;

	@Test
	void isRevokedReturnsFalseForBlank() {
		assertFalse(service.isRevoked(""));
	}

	@Test
	void revokeSkipsWhenAlreadyRevoked() {
		when(revokedTokenDao.existsByJti("jti")).thenReturn(true);
		service.revoke("jti", Instant.parse("2026-01-31T00:00:00Z"));
		verify(revokedTokenDao, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void revokePersistsWhenNew() {
		when(revokedTokenDao.existsByJti("jti")).thenReturn(false);
		Instant expiresAt = Instant.parse("2026-01-31T00:00:00Z");
		service.revoke("jti", expiresAt);

		ArgumentCaptor<RevokedTokenEntity> captor = ArgumentCaptor.forClass(RevokedTokenEntity.class);
		verify(revokedTokenDao).save(captor.capture());
		RevokedTokenEntity entity = captor.getValue();
		assertEquals("jti", entity.getJti());
		assertEquals(expiresAt, entity.getExpiresAt());
	}

	@Test
	void cleanupExpiredDeletesByCutoff() {
		Instant cutoff = Instant.parse("2026-01-31T00:00:00Z");
		service.cleanupExpired(cutoff);
		verify(revokedTokenDao).deleteByExpiresAtBefore(cutoff);
	}
}
