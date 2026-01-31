package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.domain.enumeration.AuditAction;
import com.samlair.trase.agent.rdbms.dao.AgentDao;
import com.samlair.trase.agent.rdbms.dao.TaskDao;
import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.rdbms.entity.TaskEntity;
import com.samlair.trase.agent.service.AuditService;
import com.samlair.trase.agent.service.TaskService;
import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.UpdateTaskRequestDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of task operations.
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

	private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

	private final TaskDao taskDao;
	private final AgentDao agentDao;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	@Override
	public List<TaskResponseDto> listTasks(Pageable pageable, Long afterId) {
		List<TaskEntity> pageTasks = afterId == null
				? taskDao.findAllByDeletedAtIsNull(pageable).getContent()
				: taskDao.findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(
						afterId, PageRequest.of(0, pageable.getPageSize(), Sort.by("id").ascending()));
		log.debug("Listing tasks count={}", pageTasks.size());
		if (pageTasks.isEmpty()) {
			return List.of();
		}
		List<Long> ids = pageTasks.stream()
				.map(TaskEntity::getId)
				.toList();
		Map<Long, TaskEntity> tasksById = taskDao.findAllByIdInAndDeletedAtIsNull(ids).stream()
				.collect(java.util.stream.Collectors.toMap(TaskEntity::getId, Function.identity(), (a, b) -> a));
		List<TaskResponseDto> results = new java.util.ArrayList<>(ids.size());
		for (Long id : ids) {
			TaskEntity task = tasksById.get(id);
			if (task != null) {
				results.add(toResponse(task));
			}
		}
		return results;
	}

	@Transactional
	@Override
	public TaskResponseDto createTask(CreateTaskRequestDto request) {
		TaskEntity task = new TaskEntity();
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setSupportedAgents(resolveAgents(collectAgentIds(request.supportedAgentIds(), request.supportedAgentId())));
		TaskEntity saved = taskDao.save(task);
		auditService.recordTaskAction(saved.getId(), AuditAction.CREATE);
		log.info("Created task id={}", saved.getId());
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	@Override
	public TaskResponseDto getTask(long id) {
		return toResponse(findTask(id));
	}

	@Transactional
	@Override
	public TaskResponseDto updateTask(long id, UpdateTaskRequestDto request) {
		TaskEntity task = findTask(id);
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setSupportedAgents(resolveAgents(collectAgentIds(request.supportedAgentIds(), request.supportedAgentId())));
		auditService.recordTaskAction(task.getId(), AuditAction.UPDATE);
		log.info("Updated task id={}", task.getId());
		return toResponse(task);
	}

	@Transactional
	@Override
	public void deleteTask(long id) {
		TaskEntity task = taskDao.findById(id)
				.orElseThrow(() -> new NotFoundException("Task not found: " + id));
		if (task.getDeletedAt() != null) {
			return;
		}
		task.setDeletedAt(Instant.now());
		taskDao.save(task);
		auditService.recordTaskAction(task.getId(), AuditAction.DELETE);
		log.info("Soft deleted task id={}", task.getId());
	}

	private TaskEntity findTask(long id) {
		return taskDao.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new NotFoundException("Task not found: " + id));
	}

	private Set<AgentEntity> resolveAgents(Set<Long> agentIds) {
		Set<Long> ids = new HashSet<>(agentIds);
		List<AgentEntity> agents = agentDao.findAllByIdInAndDeletedAtIsNull(ids);
		if (agents.size() != ids.size()) {
			Set<Long> resolvedIds = agents.stream().map(AgentEntity::getId).collect(java.util.stream.Collectors.toSet());
			ids.removeAll(resolvedIds);
			throw new BadRequestException("Unknown agent ids: " + ids);
		}
		return new HashSet<>(agents);
	}

	private Set<Long> collectAgentIds(Set<Long> supportedAgentIds, Long supportedAgentId) {
		Set<Long> ids = supportedAgentIds == null ? new HashSet<>() : new HashSet<>(supportedAgentIds);
		if (supportedAgentId != null) {
			ids.add(supportedAgentId);
		}
		return ids;
	}

	private TaskResponseDto toResponse(TaskEntity task) {
		Set<Long> supportedAgentIds = task.getSupportedAgents().stream()
				.map(AgentEntity::getId)
				.collect(java.util.stream.Collectors.toSet());
		Long supportedAgentId = supportedAgentIds.size() == 1 ? supportedAgentIds.iterator().next() : null;
		return new TaskResponseDto(task.getId(), task.getTitle(), task.getDescription(), supportedAgentIds, supportedAgentId);
	}
}
