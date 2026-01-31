package com.samlair.trase.agent.service;

/**
 * Scheduled cleanup for expired revoked tokens.
 */
public interface TokenRevocationCleanupJob {

	/**
	 * Deletes revoked tokens that have already expired.
	 */
	void cleanupExpiredTokens();
}
