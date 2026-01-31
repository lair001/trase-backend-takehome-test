package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.rdbms.dao.AgentAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskAuditDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunAuditDao;
import com.samlair.trase.agent.rdbms.entity.AgentAuditEntity;
import com.samlair.trase.agent.rdbms.entity.TaskAuditEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunAuditEntity;
import com.samlair.trase.agent.service.AuditQueryService;
import com.samlair.trase.agent.web.dto.AgentAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskRunAuditResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation for audit queries.
 */
@Service
@RequiredArgsConstructor
public class AuditQueryServiceImpl implements AuditQueryService {

	private static final Logger log = LoggerFactory.getLogger(AuditQueryServiceImpl.class);

	private final AgentAuditDao agentAuditDao;
	private final TaskAuditDao taskAuditDao;
	private final TaskRunAuditDao taskRunAuditDao;

	@Transactional(readOnly = true)
	@Override
	public List<AgentAuditResponseDto> listAgentAudits(Pageable pageable) {
		List<AgentAuditEntity> audits = agentAuditDao.findAll(pageable).getContent();
		log.debug("Listing agent audits count={}", audits.size());
		return audits.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	@Override
	public List<TaskAuditResponseDto> listTaskAudits(Pageable pageable) {
		List<TaskAuditEntity> audits = taskAuditDao.findAll(pageable).getContent();
		log.debug("Listing task audits count={}", audits.size());
		return audits.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	@Override
	public List<TaskRunAuditResponseDto> listTaskRunAudits(Pageable pageable) {
		List<TaskRunAuditEntity> audits = taskRunAuditDao.findAll(pageable).getContent();
		log.debug("Listing task run audits count={}", audits.size());
		return audits.stream().map(this::toResponse).toList();
	}

	private AgentAuditResponseDto toResponse(AgentAuditEntity audit) {
		return new AgentAuditResponseDto(
				audit.getId(),
				audit.getAgentId(),
				audit.getAction(),
				audit.getActorUserId(),
				audit.getActorUsername(),
				audit.getRequestId(),
				audit.getOccurredAt()
		);
	}

	private TaskAuditResponseDto toResponse(TaskAuditEntity audit) {
		return new TaskAuditResponseDto(
				audit.getId(),
				audit.getTaskId(),
				audit.getAction(),
				audit.getActorUserId(),
				audit.getActorUsername(),
				audit.getRequestId(),
				audit.getOccurredAt()
		);
	}

	private TaskRunAuditResponseDto toResponse(TaskRunAuditEntity audit) {
		return new TaskRunAuditResponseDto(
				audit.getId(),
				audit.getTaskRunId(),
				audit.getAction(),
				audit.getStatus(),
				audit.getActorUserId(),
				audit.getActorUsername(),
				audit.getRequestId(),
				audit.getOccurredAt()
		);
	}
}
