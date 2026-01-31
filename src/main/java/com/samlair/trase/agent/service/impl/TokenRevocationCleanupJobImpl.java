package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.service.TokenRevocationCleanupJob;
import com.samlair.trase.agent.service.TokenRevocationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically removes expired revoked tokens.
 */
@Component
@RequiredArgsConstructor
public class TokenRevocationCleanupJobImpl implements TokenRevocationCleanupJob {

	private final TokenRevocationService tokenRevocationService;

	/**
	 * Deletes revoked tokens that have already expired.
	 */
	@Override
	@Scheduled(cron = "${security.jwt.revocation-cleanup-cron:0 0 * * * *}")
	public void cleanupExpiredTokens() {
		tokenRevocationService.cleanupExpired(Instant.now());
	}
}
