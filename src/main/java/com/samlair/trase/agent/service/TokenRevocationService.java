package com.samlair.trase.agent.service;

import java.time.Instant;

/**
 * Provides access token revocation operations.
 */
public interface TokenRevocationService {

	/**
	 * Returns whether the token ID has been revoked.
	 *
	 * @param jti token identifier
	 * @return true if revoked
	 */
	boolean isRevoked(String jti);

	/**
	 * Records a token as revoked until it expires.
	 *
	 * @param jti token identifier
	 * @param expiresAt token expiration time
	 */
	void revoke(String jti, Instant expiresAt);

	/**
	 * Removes expired revoked tokens.
	 *
	 * @param cutoff expiration cutoff
	 */
	void cleanupExpired(Instant cutoff);
}
