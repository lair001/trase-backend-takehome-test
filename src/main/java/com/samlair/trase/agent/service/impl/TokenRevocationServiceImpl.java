package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.rdbms.dao.RevokedTokenDao;
import com.samlair.trase.agent.rdbms.entity.RevokedTokenEntity;
import com.samlair.trase.agent.service.TokenRevocationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Tracks revoked JWTs for immediate invalidation.
 */
@Service
@RequiredArgsConstructor
public class TokenRevocationServiceImpl implements TokenRevocationService {

	private final RevokedTokenDao revokedTokenDao;

	@Override
	public boolean isRevoked(String jti) {
		if (jti == null || jti.isBlank()) {
			return false;
		}
		return revokedTokenDao.existsByJti(jti);
	}

	@Override
	public void revoke(String jti, Instant expiresAt) {
		if (jti == null || jti.isBlank() || expiresAt == null) {
			return;
		}
		if (revokedTokenDao.existsByJti(jti)) {
			return;
		}
		RevokedTokenEntity entity = new RevokedTokenEntity();
		entity.setJti(jti);
		entity.setExpiresAt(expiresAt);
		revokedTokenDao.save(entity);
	}

	@Override
	public void cleanupExpired(Instant cutoff) {
		if (cutoff == null) {
			return;
		}
		revokedTokenDao.deleteByExpiresAtBefore(cutoff);
	}
}
