package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.service.TokenRevocationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenRevocationCleanupJobImplUnitTest {

	@Mock
	private TokenRevocationService tokenRevocationService;

	@InjectMocks
	private TokenRevocationCleanupJobImpl job;

	@Test
	void cleanupExpiredTokensDelegatesToService() {
		job.cleanupExpiredTokens();
		verify(tokenRevocationService).cleanupExpired(org.mockito.ArgumentMatchers.any(Instant.class));
	}
}
