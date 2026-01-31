package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.rdbms.dao.AgentDao;
import com.samlair.trase.agent.rdbms.dao.TaskDao;
import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.rdbms.entity.TaskEntity;
import com.samlair.trase.agent.service.AuditService;
import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
import com.samlair.trase.agent.web.dto.UpdateTaskRequestDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplUnitTest {

	@Mock
	private TaskDao taskDao;

	@Mock
	private AgentDao agentDao;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private TaskServiceImpl taskService;

	@Test
	void createTaskRejectsUnknownAgentIds() {
		AgentEntity agent = new AgentEntity();
		agent.setId(1L);
		when(agentDao.findAllByIdInAndDeletedAtIsNull(Set.of(1L, 2L))).thenReturn(List.of(agent));

		CreateTaskRequestDto request = new CreateTaskRequestDto(
				"Task",
				"Description",
				Set.of(1L, 2L),
				null
		);

		BadRequestException ex = assertThrows(BadRequestException.class, () -> taskService.createTask(request));
		assertTrue(ex.getMessage().contains("Unknown agent ids"));
	}

	@Test
	void updateTaskThrowsWhenMissing() {
		when(taskDao.findByIdAndDeletedAtIsNull(123L)).thenReturn(Optional.empty());
		NotFoundException ex = assertThrows(NotFoundException.class,
				() -> taskService.updateTask(123L, new UpdateTaskRequestDto(
						"Title", "Desc", Set.of(1L), null)));
		assertTrue(ex.getMessage().contains("Task not found"));
	}

	@Test
	void deleteTaskMarksDeleted() {
		TaskEntity task = new TaskEntity();
		task.setId(9L);
		when(taskDao.findById(9L)).thenReturn(Optional.of(task));

		taskService.deleteTask(9L);

		assertNotNull(task.getDeletedAt());
		verify(taskDao).save(task);
	}

	@Test
	void deleteTaskIsIdempotentWhenAlreadyDeleted() {
		TaskEntity task = new TaskEntity();
		task.setId(10L);
		task.setDeletedAt(Instant.parse("2024-01-01T00:00:00Z"));
		when(taskDao.findById(10L)).thenReturn(Optional.of(task));

		taskService.deleteTask(10L);

		verify(taskDao, never()).save(task);
	}

	@Test
	void deleteTaskThrowsWhenMissing() {
		when(taskDao.findById(404L)).thenReturn(Optional.empty());

		NotFoundException ex = assertThrows(NotFoundException.class, () -> taskService.deleteTask(404L));

		assertTrue(ex.getMessage().contains("Task not found"));
	}

	@Test
	void listTasksMapsSupportedAgentIdsInPageOrder() {
		AgentEntity agentA = new AgentEntity();
		agentA.setId(11L);
		AgentEntity agentB = new AgentEntity();
		agentB.setId(12L);

		TaskEntity taskA = new TaskEntity();
		taskA.setId(21L);
		taskA.setTitle("Task A");
		taskA.setDescription("Desc A");
		taskA.setSupportedAgents(Set.of(agentA));
		taskA.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
		taskA.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));

		TaskEntity taskB = new TaskEntity();
		taskB.setId(22L);
		taskB.setTitle("Task B");
		taskB.setDescription("Desc B");
		taskB.setSupportedAgents(Set.of(agentB));
		taskB.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
		taskB.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));

		when(taskDao.findAllByDeletedAtIsNull(Pageable.unpaged())).thenReturn(new SliceImpl<>(List.of(taskA, taskB)));
		when(taskDao.findAllByIdInAndDeletedAtIsNull(List.of(21L, 22L))).thenReturn(List.of(taskB, taskA));

		List<TaskResponseDto> response = taskService.listTasks(Pageable.unpaged(), null);

		assertEquals(2, response.size());
		assertEquals(21L, response.get(0).id());
		assertEquals(22L, response.get(1).id());
		assertEquals(Set.of(11L), response.get(0).supportedAgentIds());
		assertEquals(Set.of(12L), response.get(1).supportedAgentIds());
		assertEquals(11L, response.get(0).supportedAgentId());
		assertEquals(12L, response.get(1).supportedAgentId());
		verify(taskDao).findAllByIdInAndDeletedAtIsNull(List.of(21L, 22L));
	}

	@Test
	void listTasksHandlesDuplicateEntitiesFromSecondaryQuery() {
		AgentEntity agent = new AgentEntity();
		agent.setId(11L);

		TaskEntity taskA = new TaskEntity();
		taskA.setId(21L);
		taskA.setTitle("Task A");
		taskA.setDescription("Desc A");
		taskA.setSupportedAgents(Set.of(agent));
		TaskEntity taskADuplicate = new TaskEntity();
		taskADuplicate.setId(21L);
		taskADuplicate.setTitle("Task A");
		taskADuplicate.setDescription("Desc A");
		taskADuplicate.setSupportedAgents(Set.of(agent));

		TaskEntity taskB = new TaskEntity();
		taskB.setId(22L);
		taskB.setTitle("Task B");
		taskB.setDescription("Desc B");
		taskB.setSupportedAgents(Set.of(agent));

		when(taskDao.findAllByDeletedAtIsNull(Pageable.unpaged()))
				.thenReturn(new SliceImpl<>(List.of(taskA, taskB)));
		when(taskDao.findAllByIdInAndDeletedAtIsNull(List.of(21L, 22L)))
				.thenReturn(List.of(taskA, taskADuplicate, taskB));

		List<TaskResponseDto> response = taskService.listTasks(Pageable.unpaged(), null);

		assertEquals(2, response.size());
		assertEquals(21L, response.get(0).id());
		assertEquals(22L, response.get(1).id());
	}

	@Test
	void listTasksSkipsMissingEntitiesFromSecondaryQuery() {
		AgentEntity agent = new AgentEntity();
		agent.setId(11L);

		TaskEntity taskA = new TaskEntity();
		taskA.setId(21L);
		taskA.setTitle("Task A");
		taskA.setDescription("Desc A");
		taskA.setSupportedAgents(Set.of(agent));

		TaskEntity taskB = new TaskEntity();
		taskB.setId(22L);
		taskB.setTitle("Task B");
		taskB.setDescription("Desc B");
		taskB.setSupportedAgents(Set.of(agent));

		when(taskDao.findAllByDeletedAtIsNull(Pageable.unpaged()))
				.thenReturn(new SliceImpl<>(List.of(taskA, taskB)));
		when(taskDao.findAllByIdInAndDeletedAtIsNull(List.of(21L, 22L)))
				.thenReturn(List.of(taskA));

		List<TaskResponseDto> response = taskService.listTasks(Pageable.unpaged(), null);

		assertEquals(1, response.size());
		assertEquals(21L, response.get(0).id());
	}

	@Test
	void listTasksSupportsAfterIdKeyset() {
		AgentEntity agent = new AgentEntity();
		agent.setId(31L);

		TaskEntity task = new TaskEntity();
		task.setId(41L);
		task.setTitle("Task");
		task.setDescription("Desc");
		task.setSupportedAgents(Set.of(agent));
		task.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
		task.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));

		when(taskDao.findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(
				org.mockito.ArgumentMatchers.eq(40L),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
				.thenReturn(List.of(task));
		when(taskDao.findAllByIdInAndDeletedAtIsNull(List.of(41L))).thenReturn(List.of(task));

		List<TaskResponseDto> response = taskService.listTasks(PageRequest.of(0, 50), 40L);

		assertEquals(1, response.size());
		assertEquals(41L, response.get(0).id());
		assertEquals(Set.of(31L), response.get(0).supportedAgentIds());
		assertEquals(31L, response.get(0).supportedAgentId());
		verify(taskDao).findAllByIdInAndDeletedAtIsNull(List.of(41L));
	}

	@Test
	void getTaskReturnsResponse() {
		TaskEntity task = new TaskEntity();
		task.setId(44L);
		task.setTitle("Title");
		task.setDescription("Desc");
		task.setSupportedAgents(Set.of());
		when(taskDao.findByIdAndDeletedAtIsNull(44L)).thenReturn(Optional.of(task));

		TaskResponseDto response = taskService.getTask(44L);

		assertEquals(44L, response.id());
		assertEquals("Title", response.title());
		assertEquals("Desc", response.description());
		assertEquals(Set.of(), response.supportedAgentIds());
		assertNull(response.supportedAgentId());
	}

	@Test
	void updateTaskUpdatesFields() {
		AgentEntity agent = new AgentEntity();
		agent.setId(99L);
		TaskEntity task = new TaskEntity();
		task.setId(55L);
		task.setTitle("Old");
		task.setDescription("Old desc");
		task.setSupportedAgents(Set.of());
		when(taskDao.findByIdAndDeletedAtIsNull(55L)).thenReturn(Optional.of(task));
		when(agentDao.findAllByIdInAndDeletedAtIsNull(Set.of(99L))).thenReturn(List.of(agent));

		TaskResponseDto response = taskService.updateTask(55L, new UpdateTaskRequestDto(
				"New", "New desc", Set.of(99L), null));

		assertEquals("New", task.getTitle());
		assertEquals("New desc", task.getDescription());
		assertEquals(Set.of(agent), task.getSupportedAgents());
		assertEquals(55L, response.id());
		assertEquals("New", response.title());
		assertEquals("New desc", response.description());
		assertEquals(Set.of(99L), response.supportedAgentIds());
		assertEquals(99L, response.supportedAgentId());
	}

	@Test
	void createTaskAcceptsSingleSupportedAgentId() {
		AgentEntity agent = new AgentEntity();
		agent.setId(10L);
		when(agentDao.findAllByIdInAndDeletedAtIsNull(Set.of(10L))).thenReturn(List.of(agent));
		when(taskDao.save(org.mockito.ArgumentMatchers.any(TaskEntity.class))).thenAnswer(invocation -> {
			TaskEntity saved = invocation.getArgument(0, TaskEntity.class);
			saved.setId(100L);
			return saved;
		});

		CreateTaskRequestDto request = new CreateTaskRequestDto(
				"Task",
				"Desc",
				null,
				10L
		);

		TaskResponseDto response = taskService.createTask(request);

		assertEquals(100L, response.id());
		assertEquals(Set.of(10L), response.supportedAgentIds());
		assertEquals(10L, response.supportedAgentId());
	}
}
