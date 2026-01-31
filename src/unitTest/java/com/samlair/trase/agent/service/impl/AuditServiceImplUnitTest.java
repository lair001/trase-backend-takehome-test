package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.config.RequestIdFilter;
import com.samlair.trase.agent.domain.enumeration.AuditAction;
import com.samlair.trase.agent.rdbms.dao.AgentAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunAuditDao;
import com.samlair.trase.agent.rdbms.entity.AgentAuditEntity;
import com.samlair.trase.agent.rdbms.entity.TaskAuditEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunAuditEntity;
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
class AuditServiceImplUnitTest {

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
	void recordTaskActionCapturesUser() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("ops")
				.claim("uid", 9L)
				.claim("roles", List.of("OPERATOR"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")))
		);

		auditService.recordTaskAction(5L, AuditAction.UPDATE);

		ArgumentCaptor<TaskAuditEntity> captor = ArgumentCaptor.forClass(TaskAuditEntity.class);
		verify(taskAuditDao).save(captor.capture());
		TaskAuditEntity audit = captor.getValue();
		assertEquals(5L, audit.getTaskId());
		assertEquals("ops", audit.getActorUsername());
	}

	@Test
	void recordTaskActionHandlesMissingUserId() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("ops")
				.claim("roles", List.of("OPERATOR"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")))
		);

		auditService.recordTaskAction(6L, AuditAction.CREATE);

		ArgumentCaptor<TaskAuditEntity> captor = ArgumentCaptor.forClass(TaskAuditEntity.class);
		verify(taskAuditDao).save(captor.capture());
		TaskAuditEntity audit = captor.getValue();
		assertNull(audit.getActorUserId());
	}

	@Test
	void recordTaskRunActionCapturesStatus() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("runner")
				.claim("uid", 4L)
				.claim("roles", List.of("RUNNER"))
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_RUNNER")))
		);

		auditService.recordTaskRunAction(7L, AuditAction.STATUS_UPDATE, "COMPLETED");

		ArgumentCaptor<TaskRunAuditEntity> captor = ArgumentCaptor.forClass(TaskRunAuditEntity.class);
		verify(taskRunAuditDao).save(captor.capture());
		TaskRunAuditEntity audit = captor.getValue();
		assertEquals(7L, audit.getTaskRunId());
		assertEquals("COMPLETED", audit.getStatus());
	}

	@Test
	void recordAgentActionHandlesAnonymous() {
		auditService.recordAgentAction(3L, AuditAction.CREATE);

		ArgumentCaptor<AgentAuditEntity> captor = ArgumentCaptor.forClass(AgentAuditEntity.class);
		verify(agentAuditDao).save(captor.capture());
		AgentAuditEntity audit = captor.getValue();
		assertNull(audit.getActorUserId());
		assertNull(audit.getActorUsername());
	}
}
