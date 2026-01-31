package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.rdbms.dao.AgentDao;
import com.samlair.trase.agent.rdbms.dao.TaskDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunDao;
import com.samlair.trase.agent.rdbms.dao.TaskRunIdempotencyDao;
import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.rdbms.entity.TaskEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunIdempotencyEntity;
import com.samlair.trase.agent.service.AuditService;
import com.samlair.trase.agent.web.dto.CreateTaskRunRequestDto;
import com.samlair.trase.agent.web.dto.TaskRunResponseDto;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class TaskRunServiceImplUnitTest {

	@Mock
	private TaskRunDao taskRunDao;

	@Mock
	private TaskRunIdempotencyDao taskRunIdempotencyDao;

	@Mock
	private TaskDao taskDao;

	@Mock
	private AgentDao agentDao;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private TaskRunServiceImpl taskRunService;

	@Test
	void startTaskRunThrowsWhenTaskMissing() {
		when(taskDao.findByIdAndDeletedAtIsNullBasic(404L)).thenReturn(Optional.empty());
		NotFoundException ex = assertThrows(NotFoundException.class,
				() -> taskRunService.startTaskRun(new CreateTaskRunRequestDto(404L, 1L), null));
		assertTrue(ex.getMessage().contains("Task not found"));
	}

	@Test
	void startTaskRunThrowsWhenAgentMissing() {
		TaskEntity task = new TaskEntity();
		task.setId(10L);
		when(taskDao.findByIdAndDeletedAtIsNullBasic(10L)).thenReturn(Optional.of(task));
		when(agentDao.findByIdAndDeletedAtIsNull(404L)).thenReturn(Optional.empty());

		NotFoundException ex = assertThrows(NotFoundException.class,
				() -> taskRunService.startTaskRun(new CreateTaskRunRequestDto(10L, 404L), null));
		assertTrue(ex.getMessage().contains("Agent not found"));
	}

	@Test
	void startTaskRunRejectsUnsupportedAgent() {
		AgentEntity agent = new AgentEntity();
		agent.setId(1L);
		TaskEntity task = new TaskEntity();
		task.setId(10L);

		when(taskDao.findByIdAndDeletedAtIsNullBasic(10L)).thenReturn(Optional.of(task));
		when(agentDao.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(agent));
		when(taskDao.isAgentSupported(10L, 1L)).thenReturn(false);

		BadRequestException ex = assertThrows(BadRequestException.class,
				() -> taskRunService.startTaskRun(new CreateTaskRunRequestDto(10L, 1L), null));
		assertTrue(ex.getMessage().contains("not supported"));
		verify(taskDao).isAgentSupported(10L, 1L);
	}

	@Test
	void startTaskRunPersistsRunningTask() {
		AgentEntity agent = new AgentEntity();
		agent.setId(2L);
		TaskEntity task = new TaskEntity();
		task.setId(20L);

		when(taskDao.findByIdAndDeletedAtIsNullBasic(20L)).thenReturn(Optional.of(task));
		when(agentDao.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(agent));
		when(taskDao.isAgentSupported(20L, 2L)).thenReturn(true);
		when(taskRunDao.save(any(TaskRunEntity.class))).thenAnswer(invocation -> {
			TaskRunEntity run = invocation.getArgument(0, TaskRunEntity.class);
			if (run.getStartedAt() == null) {
				run.setStartedAt(java.time.Instant.now());
			}
			run.setId(99L);
			return run;
		});

		TaskRunResponseDto response = taskRunService.startTaskRun(new CreateTaskRunRequestDto(20L, 2L), null);

		assertEquals(99L, response.id());
		assertEquals(20L, response.taskId());
		assertEquals(2L, response.agentId());
		assertEquals(TaskRunStatus.RUNNING, response.status());
		assertNotNull(response.startedAt());
		verify(taskDao).isAgentSupported(20L, 2L);
	}

	@Test
	void startTaskRunReturnsExistingWhenIdempotencyKeyMatches() {
		AgentEntity agent = new AgentEntity();
		agent.setId(2L);
		TaskEntity task = new TaskEntity();
		task.setId(20L);

		TaskRunEntity run = new TaskRunEntity();
		run.setId(99L);
		run.setTask(task);
		run.setAgent(agent);
		run.setStatus(TaskRunStatus.RUNNING);
		run.setStartedAt(Instant.parse("2024-01-01T00:00:00Z"));

		TaskRunIdempotencyEntity idempotency = new TaskRunIdempotencyEntity();
		idempotency.setIdempotencyKey("abc123");
		idempotency.setRequestHash(hashRequest(20L, 2L));
		idempotency.setTaskRun(run);

		when(taskRunIdempotencyDao.findByIdempotencyKey("abc123")).thenReturn(Optional.of(idempotency));

		TaskRunResponseDto response = taskRunService.startTaskRun(new CreateTaskRunRequestDto(20L, 2L), "abc123");

		assertEquals(99L, response.id());
		assertEquals(20L, response.taskId());
		assertEquals(2L, response.agentId());
		verify(taskRunDao, never()).save(any(TaskRunEntity.class));
	}

	@Test
	void startTaskRunRejectsIdempotencyKeyReuseWithDifferentRequest() {
		TaskRunIdempotencyEntity idempotency = new TaskRunIdempotencyEntity();
		idempotency.setIdempotencyKey("key-1");
		idempotency.setRequestHash("different-hash");
		when(taskRunIdempotencyDao.findByIdempotencyKey("key-1")).thenReturn(Optional.of(idempotency));

		BadRequestException ex = assertThrows(BadRequestException.class,
				() -> taskRunService.startTaskRun(new CreateTaskRunRequestDto(20L, 2L), "key-1"));

		assertTrue(ex.getMessage().contains("Idempotency key already used"));
	}

	@Test
	void startTaskRunReturnsCachedWhenIdempotencySaveCollides() {
		AgentEntity agent = new AgentEntity();
		agent.setId(2L);
		TaskEntity task = new TaskEntity();
		task.setId(20L);

		TaskRunEntity savedRun = new TaskRunEntity();
		savedRun.setId(99L);
		savedRun.setTask(task);
		savedRun.setAgent(agent);
		savedRun.setStatus(TaskRunStatus.RUNNING);
		savedRun.setStartedAt(Instant.parse("2024-01-01T00:00:00Z"));

		TaskRunEntity existingRun = new TaskRunEntity();
		existingRun.setId(55L);
		existingRun.setTask(task);
		existingRun.setAgent(agent);
		existingRun.setStatus(TaskRunStatus.RUNNING);
		existingRun.setStartedAt(Instant.parse("2024-01-01T00:00:00Z"));

		TaskRunIdempotencyEntity idempotency = new TaskRunIdempotencyEntity();
		idempotency.setIdempotencyKey("key-2");
		idempotency.setRequestHash(hashRequest(20L, 2L));
		idempotency.setTaskRun(existingRun);

		when(taskDao.findByIdAndDeletedAtIsNullBasic(20L)).thenReturn(Optional.of(task));
		when(agentDao.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(agent));
		when(taskDao.isAgentSupported(20L, 2L)).thenReturn(true);
		when(taskRunDao.save(any(TaskRunEntity.class))).thenReturn(savedRun);
		when(taskRunIdempotencyDao.save(any(TaskRunIdempotencyEntity.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));
		when(taskRunIdempotencyDao.findByIdempotencyKey("key-2"))
				.thenReturn(Optional.empty(), Optional.of(idempotency));

		TaskRunResponseDto response = taskRunService.startTaskRun(new CreateTaskRunRequestDto(20L, 2L), "key-2");

		assertEquals(55L, response.id());
	}

	@Test
	void startTaskRunPropagatesIdempotencyCollisionWithoutCache() {
		AgentEntity agent = new AgentEntity();
		agent.setId(2L);
		TaskEntity task = new TaskEntity();
		task.setId(20L);

		when(taskDao.findByIdAndDeletedAtIsNullBasic(20L)).thenReturn(Optional.of(task));
		when(agentDao.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(agent));
		when(taskDao.isAgentSupported(20L, 2L)).thenReturn(true);
		when(taskRunDao.save(any(TaskRunEntity.class))).thenAnswer(invocation -> {
			TaskRunEntity run = invocation.getArgument(0, TaskRunEntity.class);
			run.setId(88L);
			run.setStartedAt(Instant.parse("2024-01-01T00:00:00Z"));
			return run;
		});
		when(taskRunIdempotencyDao.save(any(TaskRunIdempotencyEntity.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));
		when(taskRunIdempotencyDao.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());

		assertThrows(DataIntegrityViolationException.class,
				() -> taskRunService.startTaskRun(new CreateTaskRunRequestDto(20L, 2L), "key-3"));
	}

	@Test
	void startTaskRunIgnoresBlankIdempotencyKey() {
		AgentEntity agent = new AgentEntity();
		agent.setId(2L);
		TaskEntity task = new TaskEntity();
		task.setId(20L);

		when(taskDao.findByIdAndDeletedAtIsNullBasic(20L)).thenReturn(Optional.of(task));
		when(agentDao.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(agent));
		when(taskDao.isAgentSupported(20L, 2L)).thenReturn(true);
		when(taskRunDao.save(any(TaskRunEntity.class))).thenAnswer(invocation -> {
			TaskRunEntity run = invocation.getArgument(0, TaskRunEntity.class);
			run.setId(77L);
			run.setStartedAt(Instant.parse("2024-01-01T00:00:00Z"));
			return run;
		});

		TaskRunResponseDto response = taskRunService.startTaskRun(new CreateTaskRunRequestDto(20L, 2L), "  ");

		assertEquals(77L, response.id());
		verify(taskRunIdempotencyDao, never()).findByIdempotencyKey(any());
	}

	@Test
	void startTaskRunRejectsTooLongIdempotencyKey() {
		String key = "a".repeat(129);
		BadRequestException ex = assertThrows(BadRequestException.class,
				() -> taskRunService.startTaskRun(new CreateTaskRunRequestDto(1L, 2L), key));
		assertTrue(ex.getMessage().contains("Idempotency key too long"));
	}

	@Test
	void updateTaskRunStatusThrowsWhenMissing() {
		when(taskRunDao.findById(404L)).thenReturn(Optional.empty());

		NotFoundException ex = assertThrows(NotFoundException.class,
				() -> taskRunService.updateTaskRunStatus(404L, TaskRunStatus.COMPLETED));

		assertTrue(ex.getMessage().contains("Task run not found"));
	}

	@Test
	void updateTaskRunStatusRejectsNonRunning() {
		TaskRunEntity run = new TaskRunEntity();
		run.setId(1L);
		run.setStatus(TaskRunStatus.COMPLETED);
		when(taskRunDao.findById(1L)).thenReturn(Optional.of(run));

		BadRequestException ex = assertThrows(BadRequestException.class,
				() -> taskRunService.updateTaskRunStatus(1L, TaskRunStatus.RUNNING));

		assertTrue(ex.getMessage().contains("is not running"));
	}

	@Test
	void listTaskRunsSupportsAfterIdKeyset() {
		TaskRunEntity run = new TaskRunEntity();
		run.setId(15L);
		run.setStatus(TaskRunStatus.RUNNING);
		run.setStartedAt(Instant.parse("2024-01-01T00:00:00Z"));
		TaskEntity task = new TaskEntity();
		task.setId(20L);
		AgentEntity agent = new AgentEntity();
		agent.setId(30L);
		run.setTask(task);
		run.setAgent(agent);

		when(taskRunDao.findAllByIdGreaterThanOrderByIdAsc(
				org.mockito.ArgumentMatchers.eq(10L),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
				.thenReturn(java.util.List.of(run));

		java.util.List<TaskRunResponseDto> response = taskRunService.listTaskRuns(null, PageRequest.of(0, 50), 10L);

		assertEquals(1, response.size());
		assertEquals(15L, response.get(0).id());
		assertEquals(TaskRunStatus.RUNNING, response.get(0).status());
	}

	@Test
	void listTaskRunsSupportsAfterIdKeysetWithStatus() {
		TaskRunEntity run = new TaskRunEntity();
		run.setId(25L);
		run.setStatus(TaskRunStatus.COMPLETED);
		TaskEntity task = new TaskEntity();
		task.setId(21L);
		AgentEntity agent = new AgentEntity();
		agent.setId(31L);
		run.setTask(task);
		run.setAgent(agent);
		run.setStartedAt(Instant.parse("2024-01-01T00:00:00Z"));

		when(taskRunDao.findByStatusAndIdGreaterThanOrderByIdAsc(
				org.mockito.ArgumentMatchers.eq(TaskRunStatus.COMPLETED),
				org.mockito.ArgumentMatchers.eq(20L),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
				.thenReturn(java.util.List.of(run));

		java.util.List<TaskRunResponseDto> response = taskRunService.listTaskRuns(
				TaskRunStatus.COMPLETED, PageRequest.of(0, 10), 20L);

		assertEquals(1, response.size());
		assertEquals(25L, response.get(0).id());
		assertEquals(TaskRunStatus.COMPLETED, response.get(0).status());
	}

	@Test
	void hashRequestThrowsWhenDigestUnavailable() throws Exception {
		try (MockedStatic<MessageDigest> mocked = mockStatic(MessageDigest.class)) {
			mocked.when(() -> MessageDigest.getInstance("SHA-256"))
					.thenThrow(new NoSuchAlgorithmException("nope"));

			var method = TaskRunServiceImpl.class.getDeclaredMethod("hashRequest", CreateTaskRunRequestDto.class);
			method.setAccessible(true);
			InvocationTargetException ex = assertThrows(InvocationTargetException.class,
					() -> method.invoke(taskRunService, new CreateTaskRunRequestDto(1L, 2L)));
			assertTrue(ex.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	void updateTaskRunStatusSetsCompletedAt() {
		TaskRunEntity run = new TaskRunEntity();
		run.setId(2L);
		run.setStatus(TaskRunStatus.RUNNING);
		run.setStartedAt(Instant.now());
		TaskEntity task = new TaskEntity();
		task.setId(20L);
		AgentEntity agent = new AgentEntity();
		agent.setId(30L);
		run.setTask(task);
		run.setAgent(agent);
		when(taskRunDao.findById(2L)).thenReturn(Optional.of(run));
		when(taskRunDao.save(any(TaskRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TaskRunResponseDto response = taskRunService.updateTaskRunStatus(2L, TaskRunStatus.COMPLETED);

		assertEquals(TaskRunStatus.COMPLETED, response.status());
		assertNotNull(response.completedAt());
	}

	@Test
	void updateTaskRunStatusReturnsWhenStatusUnchanged() {
		TaskRunEntity run = new TaskRunEntity();
		run.setId(3L);
		run.setStatus(TaskRunStatus.RUNNING);
		run.setStartedAt(Instant.now());
		TaskEntity task = new TaskEntity();
		task.setId(21L);
		AgentEntity agent = new AgentEntity();
		agent.setId(31L);
		run.setTask(task);
		run.setAgent(agent);
		when(taskRunDao.findById(3L)).thenReturn(Optional.of(run));

		TaskRunResponseDto response = taskRunService.updateTaskRunStatus(3L, TaskRunStatus.RUNNING);

		assertEquals(TaskRunStatus.RUNNING, response.status());
		assertEquals(3L, response.id());
		assertEquals(21L, response.taskId());
		assertEquals(31L, response.agentId());
	}

	@Test
	void updateTaskRunStatusKeepsCompletedAtWhenAlreadySet() {
		TaskRunEntity run = new TaskRunEntity();
		run.setId(4L);
		run.setStatus(TaskRunStatus.RUNNING);
		run.setStartedAt(Instant.now());
		Instant completedAt = Instant.parse("2024-01-02T00:00:00Z");
		run.setCompletedAt(completedAt);
		TaskEntity task = new TaskEntity();
		task.setId(22L);
		AgentEntity agent = new AgentEntity();
		agent.setId(32L);
		run.setTask(task);
		run.setAgent(agent);
		when(taskRunDao.findById(4L)).thenReturn(Optional.of(run));
		when(taskRunDao.save(any(TaskRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TaskRunResponseDto response = taskRunService.updateTaskRunStatus(4L, TaskRunStatus.COMPLETED);

		assertEquals(TaskRunStatus.COMPLETED, response.status());
		assertEquals(completedAt, response.completedAt());
	}

	private static String hashRequest(long taskId, long agentId) {
		String payload = taskId + ":" + agentId;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(bytes.length * 2);
			for (byte value : bytes) {
				builder.append(String.format("%02x", value));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
