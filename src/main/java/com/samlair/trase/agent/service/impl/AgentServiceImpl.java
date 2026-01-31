package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.domain.enumeration.AuditAction;
import com.samlair.trase.agent.rdbms.dao.AgentDao;
import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.service.AgentService;
import com.samlair.trase.agent.service.AuditService;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.UpdateAgentRequestDto;
import com.samlair.trase.agent.web.dto.AgentResponseDto;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of agent operations.
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

	private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

	private final AgentDao agentDao;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	@Override
	public List<AgentResponseDto> listAgents(Pageable pageable, Long afterId) {
		List<AgentEntity> agents = afterId == null
				? agentDao.findAllByDeletedAtIsNull(pageable).getContent()
				: agentDao.findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(
						afterId, PageRequest.of(0, pageable.getPageSize(), Sort.by("id").ascending()));
		log.debug("Listing agents count={}", agents.size());
		return agents.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	@Override
	public AgentResponseDto createAgent(CreateAgentRequestDto request) {
		assertUniqueName(request.name(), null);
		AgentEntity agent = new AgentEntity();
		agent.setName(request.name());
		agent.setDescription(request.description());
		AgentEntity saved = agentDao.save(agent);
		auditService.recordAgentAction(saved.getId(), AuditAction.CREATE);
		log.info("Created agent id={}", saved.getId());
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	@Override
	public AgentResponseDto getAgent(long id) {
		return toResponse(findAgent(id));
	}

	@Transactional
	@Override
	public AgentResponseDto updateAgent(long id, UpdateAgentRequestDto request) {
		AgentEntity agent = findAgent(id);
		assertUniqueName(request.name(), id);
		agent.setName(request.name());
		agent.setDescription(request.description());
		auditService.recordAgentAction(agent.getId(), AuditAction.UPDATE);
		log.info("Updated agent id={}", agent.getId());
		return toResponse(agent);
	}

	@Transactional
	@Override
	public void deleteAgent(long id) {
		AgentEntity agent = agentDao.findById(id)
				.orElseThrow(() -> new NotFoundException("Agent not found: " + id));
		if (agent.getDeletedAt() != null) {
			return;
		}
		agent.setDeletedAt(Instant.now());
		agentDao.save(agent);
		auditService.recordAgentAction(agent.getId(), AuditAction.DELETE);
		log.info("Soft deleted agent id={}", agent.getId());
	}

	private AgentEntity findAgent(long id) {
		return agentDao.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new NotFoundException("Agent not found: " + id));
	}

	private AgentResponseDto toResponse(AgentEntity agent) {
		return new AgentResponseDto(agent.getId(), agent.getName(), agent.getDescription());
	}

	private void assertUniqueName(String name, Long existingId) {
		boolean exists = existingId == null
				? agentDao.existsByNameAndDeletedAtIsNull(name)
				: agentDao.existsByNameAndDeletedAtIsNullAndIdNot(name, existingId);
		if (exists) {
			throw new BadRequestException("Agent name already exists: " + name);
		}
	}
}
