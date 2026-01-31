package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.RevokedTokenEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for revoked tokens.
 */
public interface RevokedTokenDao extends JpaRepository<RevokedTokenEntity, Long> {

	boolean existsByJti(String jti);

	int deleteByExpiresAtBefore(Instant cutoff);
}
