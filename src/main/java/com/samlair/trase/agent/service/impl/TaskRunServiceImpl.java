package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.domain.enumeration.AuditAction;
import com.samlair.trase.agent.rdbms.dao.AgentDao;
import com.samlair.trase.agent.rdbms.dao.TaskDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunIdempotencyDao;
import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.rdbms.entity.TaskEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunIdempotencyEntity;
import com.samlair.trase.agent.service.AuditService;
import com.samlair.trase.agent.service.TaskRunService;
import com.samlair.trase.agent.web.dto.CreateTaskRunRequestDto;
import com.samlair.trase.agent.web.dto.TaskRunResponseDto;
import java.time.Instant;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of task run operations.
 */
@Service
@RequiredArgsConstructor
public class TaskRunServiceImpl implements TaskRunService {

	private static final Logger log = LoggerFactory.getLogger(TaskRunServiceImpl.class);

	private final TaskRunDao taskRunDao;
	private final TaskDao taskDao;
	private final AgentDao agentDao;
	private final TaskRunIdempotencyDao taskRunIdempotencyDao;
	private final AuditService auditService;

	@Transactional
	@Override
	public TaskRunResponseDto startTaskRun(CreateTaskRunRequestDto request, String idempotencyKey) {
		String normalizedKey = normalizeKey(idempotencyKey);
		if (normalizedKey != null) {
			TaskRunResponseDto cached = maybeReturnIdempotentResponse(request, normalizedKey);
			if (cached != null) {
				return cached;
			}
		}

		TaskEntity task = taskDao.findByIdAndDeletedAtIsNullBasic(request.taskId())
				.orElseThrow(() -> new NotFoundException("Task not found: " + request.taskId()));
		AgentEntity agent = agentDao.findByIdAndDeletedAtIsNull(request.agentId())
				.orElseThrow(() -> new NotFoundException("Agent not found: " + request.agentId()));
		if (!taskDao.isAgentSupported(task.getId(), agent.getId())) {
			throw new BadRequestException("Agent " + agent.getId() + " is not supported for task " + task.getId());
		}

		TaskRunEntity taskRun = new TaskRunEntity();
		taskRun.setTask(task);
		taskRun.setAgent(agent);
		taskRun.setStatus(TaskRunStatus.RUNNING);
		TaskRunEntity saved = taskRunDao.save(taskRun);
		if (normalizedKey != null) {
			TaskRunResponseDto cached = persistIdempotencyKey(normalizedKey, request, saved);
			if (cached != null) {
				return cached;
			}
		}
		auditService.recordTaskRunAction(saved.getId(), AuditAction.START, saved.getStatus().name());
		log.info("Started task run id={} taskId={} agentId={}", saved.getId(), task.getId(), agent.getId());
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	@Override
	public List<TaskRunResponseDto> listTaskRuns(TaskRunStatus status, Pageable pageable, Long afterId) {
		PageRequest keysetPageable = PageRequest.of(0, pageable.getPageSize(), Sort.by("id").ascending());
		List<TaskRunEntity> runs;
		if (afterId == null) {
				runs = status == null
						? taskRunDao.findAllBy(pageable).getContent()
						: taskRunDao.findByStatus(status, pageable).getContent();
		} else {
			runs = status == null
					? taskRunDao.findAllByIdGreaterThanOrderByIdAsc(afterId, keysetPageable)
					: taskRunDao.findByStatusAndIdGreaterThanOrderByIdAsc(status, afterId, keysetPageable);
		}
		log.debug("Listing task runs status={} count={}", status, runs.size());
		return runs.stream().map(this::toResponse).toList();
	}

	@Transactional
	@Override
	public TaskRunResponseDto updateTaskRunStatus(long id, TaskRunStatus status) {
		TaskRunEntity run = taskRunDao.findById(id)
				.orElseThrow(() -> new NotFoundException("Task run not found: " + id));
		if (run.getStatus() != TaskRunStatus.RUNNING) {
			throw new BadRequestException("Task run " + id + " is not running");
		}
		if (run.getStatus() == status) {
			return toResponse(run);
		}
		run.setStatus(status);
		if (run.getCompletedAt() == null) {
			run.setCompletedAt(Instant.now());
		}
		TaskRunEntity saved = taskRunDao.save(run);
		auditService.recordTaskRunAction(saved.getId(), AuditAction.STATUS_UPDATE, saved.getStatus().name());
		return toResponse(saved);
	}

	private TaskRunResponseDto toResponse(TaskRunEntity run) {
		return new TaskRunResponseDto(
				run.getId(),
				run.getTask().getId(),
				run.getAgent().getId(),
				run.getStatus(),
				run.getStartedAt(),
				run.getCompletedAt()
		);
	}

	private TaskRunResponseDto maybeReturnIdempotentResponse(CreateTaskRunRequestDto request, String key) {
		return taskRunIdempotencyDao.findByIdempotencyKey(key)
				.map(existing -> {
					String requestHash = hashRequest(request);
					if (!requestHash.equals(existing.getRequestHash())) {
						throw new BadRequestException("Idempotency key already used for different request");
					}
					TaskRunEntity run = existing.getTaskRun();
					return toResponse(run);
				})
				.orElse(null);
	}

	private TaskRunResponseDto persistIdempotencyKey(String key, CreateTaskRunRequestDto request, TaskRunEntity run) {
		TaskRunIdempotencyEntity entity = new TaskRunIdempotencyEntity();
		entity.setIdempotencyKey(key);
		entity.setRequestHash(hashRequest(request));
		entity.setTaskRun(run);
		try {
			taskRunIdempotencyDao.save(entity);
			return null;
		} catch (DataIntegrityViolationException ex) {
			TaskRunResponseDto cached = maybeReturnIdempotentResponse(request, key);
			if (cached != null) {
				return cached;
			}
			throw ex;
		}
	}

	private String normalizeKey(String key) {
		if (key == null) {
			return null;
		}
		String trimmed = key.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() > 128) {
			throw new BadRequestException("Idempotency key too long");
		}
		return trimmed;
	}

	private String hashRequest(CreateTaskRunRequestDto request) {
		String payload = request.taskId() + ":" + request.agentId();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(bytes.length * 2);
			for (byte value : bytes) {
				builder.append(String.format("%02x", value));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}
