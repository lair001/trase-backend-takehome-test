package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.IntegrationTestBase;
import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.CreateTaskRunRequestDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
import com.samlair.trase.agent.web.dto.TaskRunResponseDto;
import com.samlair.trase.agent.web.dto.UpdateTaskRunStatusRequestDto;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskRunControllerIntTest extends IntegrationTestBase {

	@Test
	void startTaskRunReturnsNotFoundForMissingTask() {
		Long agentId = createAgent("Agent A").id();

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(99999L, agentId))
				.retrieve()
				.toEntity(String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void startTaskRunReturnsNotFoundForMissingAgent() {
		Long agentId = createAgent("Agent A").id();
		TaskResponseDto task = createTask("Task", Set.of(agentId));

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), 12345L))
				.retrieve()
				.toEntity(String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void startTaskRunRejectsUnsupportedAgent() {
		Long supportedAgentId = createAgent("Agent A").id();
		Long unsupportedAgentId = createAgent("Agent B").id();
		TaskResponseDto task = createTask("Task", Set.of(supportedAgentId));

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), unsupportedAgentId))
				.retrieve()
				.toEntity(String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void startTaskRunHonorsIdempotencyKey() {
		Long agentId = createAgent("Agent A").id();
		TaskResponseDto task = createTask("Task", Set.of(agentId));

		TaskRunResponseDto first = restClient.post()
				.uri("/task-runs")
				.header("Idempotency-Key", "idem-key-1")
				.body(new CreateTaskRunRequestDto(task.id(), agentId))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();

		TaskRunResponseDto second = restClient.post()
				.uri("/task-runs")
				.header("Idempotency-Key", "idem-key-1")
				.body(new CreateTaskRunRequestDto(task.id(), agentId))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();

		assertThat(first).isNotNull();
		assertThat(second).isNotNull();
		assertThat(second.id()).isEqualTo(first.id());
	}

	@Test
	void listTaskRunsFiltersByStatus() {
		Long agentId = createAgent("Agent A").id();
		TaskResponseDto task = createTask("Task", Set.of(agentId));

		ResponseEntity<TaskRunResponseDto> run = restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), agentId))
				.retrieve()
				.toEntity(TaskRunResponseDto.class);
		assertThat(run.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		ResponseEntity<List<TaskRunResponseDto>> runningRuns = restClient.get()
				.uri("/task-runs?status=RUNNING")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});
		assertThat(runningRuns.getBody()).hasSize(1);
		assertThat(runningRuns.getBody().get(0).status()).isEqualTo(TaskRunStatus.RUNNING);

		ResponseEntity<List<TaskRunResponseDto>> completedRuns = restClient.get()
				.uri("/task-runs?status=COMPLETED")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});
		assertThat(completedRuns.getBody()).isEmpty();

		ResponseEntity<List<TaskRunResponseDto>> allRuns = restClient.get()
				.uri("/task-runs")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});
		assertThat(allRuns.getBody()).hasSize(1);
	}

	@Test
	void listTaskRunsSupportsOffsetPaging() {
		Long agentId = createAgent("Agent A").id();
		TaskResponseDto task = createTask("Task", Set.of(agentId));

		TaskRunResponseDto first = restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), agentId))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();
		TaskRunResponseDto second = restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), agentId))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();

		assertThat(first).isNotNull();
		assertThat(second).isNotNull();

		ResponseEntity<List<TaskRunResponseDto>> page1 = restClient.get()
				.uri("/task-runs?page=1&size=1&sort=id,asc")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(page1.getBody()).hasSize(1);
		assertThat(page1.getBody().get(0).id()).isEqualTo(second.id());
	}

	@Test
	void updateTaskRunStatusCompletesRun() {
		Long agentId = createAgent("Agent A").id();
		TaskResponseDto task = createTask("Task", Set.of(agentId));

		TaskRunResponseDto run = restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), agentId))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();
		assertThat(run).isNotNull();

		TaskRunResponseDto updated = restClient.patch()
				.uri("/task-runs/" + run.id())
				.body(new UpdateTaskRunStatusRequestDto(TaskRunStatus.COMPLETED))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();

		assertThat(updated).isNotNull();
		assertThat(updated.status()).isEqualTo(TaskRunStatus.COMPLETED);
		assertThat(updated.completedAt()).isNotNull();
	}

	@Test
	void updateTaskRunStatusRejectsNonRunning() {
		Long agentId = createAgent("Agent A").id();
		TaskResponseDto task = createTask("Task", Set.of(agentId));

		TaskRunResponseDto run = restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), agentId))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();
		assertThat(run).isNotNull();

		restClient.patch()
				.uri("/task-runs/" + run.id())
				.body(new UpdateTaskRunStatusRequestDto(TaskRunStatus.COMPLETED))
				.retrieve()
				.toEntity(TaskRunResponseDto.class);

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.patch()
				.uri("/task-runs/" + run.id())
				.body(new UpdateTaskRunStatusRequestDto(TaskRunStatus.RUNNING))
				.retrieve()
				.toEntity(String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	private com.samlair.trase.agent.web.dto.AgentResponseDto createAgent(String name) {
		return restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto(name, "desc"))
				.retrieve()
				.toEntity(com.samlair.trase.agent.web.dto.AgentResponseDto.class)
				.getBody();
	}

	private TaskResponseDto createTask(String title, Set<Long> supportedAgents) {
		return restClient.post()
				.uri("/tasks")
				.body(new CreateTaskRequestDto(title, "desc", supportedAgents, null))
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();
	}
}
