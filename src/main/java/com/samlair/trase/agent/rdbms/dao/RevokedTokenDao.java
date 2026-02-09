package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.RevokedTokenEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for revoked tokens.
 */
public interface RevokedTokenDao extends JpaRepository<RevokedTokenEntity, Long> {

	/**
	 * Checks if a JWT id has been revoked.
	 *
	 * @param jti JWT id.
	 * @return true when the token is revoked.
	 */
	boolean existsByJti(String jti);

	/**
	 * Deletes revoked tokens that expired before the cutoff.
	 *
	 * @param cutoff expiration cutoff.
	 * @return number of rows deleted.
	 */
	int deleteByExpiresAtBefore(Instant cutoff);
}
