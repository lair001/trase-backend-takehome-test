package com.samlair.trase.agent.service;

import com.samlair.trase.agent.config.RequestIdFilter;
import com.samlair.trase.agent.domain.enumeration.AuditAction;
import com.samlair.trase.agent.rdbms.dao.AgentAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunAuditDao;
import com.samlair.trase.agent.rdbms.entity.AgentAuditEntity;
import com.samlair.trase.agent.service.impl.AuditServiceImpl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceUnitTest {

	@Mock
	private AgentAuditDao agentAuditDao;

	@Mock
	private TaskAuditDao taskAuditDao;

	@Mock
	private TaskRunAuditDao taskRunAuditDao;

	@InjectMocks
	private AuditServiceImpl auditService;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		MDC.remove(RequestIdFilter.REQUEST_ID_MDC_KEY);
	}

	@Test
	void recordAgentActionCapturesNumericUserId() {
		MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, "req-1");
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("admin")
				.claim("uid", 42L)
				.claim("roles", List.of("ADMIN"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
		);

		auditService.recordAgentAction(10L, AuditAction.CREATE);

		ArgumentCaptor<AgentAuditEntity> captor = ArgumentCaptor.forClass(AgentAuditEntity.class);
		verify(agentAuditDao).save(captor.capture());
		AgentAuditEntity audit = captor.getValue();
		assertEquals(42L, audit.getActorUserId());
		assertEquals("admin", audit.getActorUsername());
		assertEquals("req-1", audit.getRequestId());
	}

	@Test
	void recordAgentActionCapturesStringUserId() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("reader")
				.claim("uid", "7")
				.claim("roles", List.of("READER"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_READER")))
		);

		auditService.recordAgentAction(11L, AuditAction.UPDATE);

		ArgumentCaptor<AgentAuditEntity> captor = ArgumentCaptor.forClass(AgentAuditEntity.class);
		verify(agentAuditDao).save(captor.capture());
		AgentAuditEntity audit = captor.getValue();
		assertEquals(7L, audit.getActorUserId());
		assertEquals("reader", audit.getActorUsername());
	}

	@Test
	void recordAgentActionHandlesAnonymous() {
		auditService.recordAgentAction(12L, AuditAction.DELETE);

		ArgumentCaptor<AgentAuditEntity> captor = ArgumentCaptor.forClass(AgentAuditEntity.class);
		verify(agentAuditDao).save(captor.capture());
		AgentAuditEntity audit = captor.getValue();
		assertNull(audit.getActorUserId());
		assertNull(audit.getActorUsername());
	}
}
