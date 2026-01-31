package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.config.RequestIdFilter;
import com.samlair.trase.agent.domain.enumeration.AuditAction;
import com.samlair.trase.agent.rdbms.dao.AgentAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunAuditDao;
import com.samlair.trase.agent.rdbms.entity.AgentAuditEntity;
import com.samlair.trase.agent.rdbms.entity.TaskAuditEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunAuditEntity;
import com.samlair.trase.agent.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Records audit events for write operations.
 */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

	private final AgentAuditDao agentAuditDao;
	private final TaskAuditDao taskAuditDao;
	private final TaskRunAuditDao taskRunAuditDao;

	@Override
	public void recordAgentAction(Long agentId, AuditAction action) {
		AuditActor actor = currentActor();
		AgentAuditEntity audit = new AgentAuditEntity();
		audit.setAgentId(agentId);
		audit.setAction(action);
		audit.setActorUserId(actor.userId());
		audit.setActorUsername(actor.username());
		audit.setRequestId(actor.requestId());
		agentAuditDao.save(audit);
	}

	@Override
	public void recordTaskAction(Long taskId, AuditAction action) {
		AuditActor actor = currentActor();
		TaskAuditEntity audit = new TaskAuditEntity();
		audit.setTaskId(taskId);
		audit.setAction(action);
		audit.setActorUserId(actor.userId());
		audit.setActorUsername(actor.username());
		audit.setRequestId(actor.requestId());
		taskAuditDao.save(audit);
	}

	@Override
	public void recordTaskRunAction(Long taskRunId, AuditAction action, String status) {
		AuditActor actor = currentActor();
		TaskRunAuditEntity audit = new TaskRunAuditEntity();
		audit.setTaskRunId(taskRunId);
		audit.setAction(action);
		audit.setStatus(status);
		audit.setActorUserId(actor.userId());
		audit.setActorUsername(actor.username());
		audit.setRequestId(actor.requestId());
		taskRunAuditDao.save(audit);
	}

	private AuditActor currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			Jwt jwt = token.getToken();
			Object uid = jwt.getClaim("uid");
			Long userId = null;
			if (uid instanceof Number number) {
				userId = number.longValue();
			} else if (uid instanceof String value) {
				userId = Long.parseLong(value);
			}
			String username = jwt.getSubject();
			String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
			return new AuditActor(userId, username, requestId);
		}
		return new AuditActor(null, null, MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
	}

	private record AuditActor(Long userId, String username, String requestId) {
	}
}
