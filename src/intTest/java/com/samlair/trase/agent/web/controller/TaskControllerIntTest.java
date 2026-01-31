package com.samlair.trase.agent.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.samlair.trase.agent.IntegrationTestBase;
import com.samlair.trase.agent.web.dto.ApiErrorDto;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
import com.samlair.trase.agent.web.dto.UpdateTaskRequestDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskControllerIntTest extends IntegrationTestBase {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule());

	@Test
	void createTaskValidatesInput() throws Exception {
		CreateTaskRequestDto request = new CreateTaskRequestDto("", "", Set.of(), null);

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.post()
				.uri("/tasks")
				.body(request)
				.retrieve()
				.toEntity(String.class));

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		ApiErrorDto error = readError(ex);
		assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(error.error()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
		assertThat(error.message()).isEqualTo("Validation failed");
		assertThat(error.path()).isEqualTo("/tasks");
		assertThat(error.validationErrors()).containsKeys("title", "description");
		assertThat(error.validationErrors().keySet()).contains("supportedAgentValid");
	}

	@Test
	void createTaskAcceptsSingleSupportedAgentId() {
		Long agentId = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent", "Agent desc"))
				.retrieve()
				.toEntity(com.samlair.trase.agent.web.dto.AgentResponseDto.class)
				.getBody()
				.id();

		Map<String, Object> request = new HashMap<>();
		request.put("title", "Task");
		request.put("description", "Task desc");
		request.put("supported_agent_id", agentId);

		TaskResponseDto task = restClient.post()
				.uri("/tasks")
				.body(request)
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		assertThat(task).isNotNull();
		assertThat(task.supportedAgentIds()).containsExactly(agentId);
		assertThat(task.supportedAgentId()).isEqualTo(agentId);
	}

	@Test
	void getTaskReturnsTask() {
		Long agentId = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent", "Agent desc"))
				.retrieve()
				.toEntity(com.samlair.trase.agent.web.dto.AgentResponseDto.class)
				.getBody()
				.id();

		TaskResponseDto task = restClient.post()
				.uri("/tasks")
				.body(new CreateTaskRequestDto("Task", "Task desc", Set.of(agentId), null))
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		TaskResponseDto fetched = restClient.get()
				.uri("/tasks/" + task.id())
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		assertThat(fetched).isNotNull();
		assertThat(fetched.id()).isEqualTo(task.id());
		assertThat(fetched.supportedAgentIds()).containsExactly(agentId);
		assertThat(fetched.supportedAgentId()).isEqualTo(agentId);
	}

	@Test
	void updateTaskUpdatesResponse() {
		Long agentId = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent", "Agent desc"))
				.retrieve()
				.toEntity(com.samlair.trase.agent.web.dto.AgentResponseDto.class)
				.getBody()
				.id();

		TaskResponseDto task = restClient.post()
				.uri("/tasks")
				.body(new CreateTaskRequestDto("Task", "Before", Set.of(agentId), null))
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		TaskResponseDto updated = restClient.put()
				.uri("/tasks/" + task.id())
				.body(new UpdateTaskRequestDto("Task", "After", Set.of(agentId), null))
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		assertThat(updated).isNotNull();
		assertThat(updated.description()).isEqualTo("After");
		assertThat(updated.supportedAgentId()).isEqualTo(agentId);
	}

	@Test
	void deleteTaskRemovesFromQueries() {
		CreateAgentRequestDto agentRequest = new CreateAgentRequestDto("Agent", "Agent desc");
		Long agentId = restClient.post()
				.uri("/agents")
				.body(agentRequest)
				.retrieve()
				.toEntity(com.samlair.trase.agent.web.dto.AgentResponseDto.class)
				.getBody()
				.id();

		CreateTaskRequestDto taskRequest = new CreateTaskRequestDto(
				"Task", "Task desc", Set.of(agentId), null);
		TaskResponseDto task = restClient.post()
				.uri("/tasks")
				.body(taskRequest)
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();
		assertThat(task).isNotNull();

		restClient.delete()
				.uri("/tasks/" + task.id())
				.retrieve()
				.toBodilessEntity();

		HttpClientErrorException getEx = assertThrows(HttpClientErrorException.class, () -> restClient.get()
				.uri("/tasks/" + task.id())
				.retrieve()
				.toEntity(String.class));
		assertThat(getEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<List<TaskResponseDto>> listed = restClient.get()
				.uri("/tasks")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});
		assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listed.getBody()).isEmpty();
	}

	@Test
	void updateTaskRejectsMissingTask() {
		UpdateTaskRequestDto update = new UpdateTaskRequestDto("Title", "Desc", Set.of(1L), null);
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.put()
				.uri("/tasks/99999")
				.body(update)
				.retrieve()
				.toEntity(String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void listTasksReturnsPageWithSupportedAgents() {
		Long agentId = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent", "Agent desc"))
				.retrieve()
				.toEntity(com.samlair.trase.agent.web.dto.AgentResponseDto.class)
				.getBody()
				.id();

		TaskResponseDto taskA = restClient.post()
				.uri("/tasks")
				.body(new CreateTaskRequestDto("Task A", "Desc A", Set.of(agentId), null))
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();
		TaskResponseDto taskB = restClient.post()
				.uri("/tasks")
				.body(new CreateTaskRequestDto("Task B", "Desc B", Set.of(agentId), null))
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		ResponseEntity<List<TaskResponseDto>> page = restClient.get()
				.uri("/tasks?size=1&sort=id,asc")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(page.getBody()).hasSize(1);
		TaskResponseDto first = page.getBody().get(0);
		assertThat(first.id()).isEqualTo(taskA.id());
		assertThat(first.supportedAgentIds()).containsExactly(agentId);
		assertThat(first.supportedAgentId()).isEqualTo(agentId);

		ResponseEntity<List<TaskResponseDto>> page2 = restClient.get()
				.uri("/tasks?page=1&size=1&sort=id,asc")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(page2.getBody()).hasSize(1);
		assertThat(page2.getBody().get(0).id()).isEqualTo(taskB.id());
		assertThat(page2.getBody().get(0).supportedAgentIds()).containsExactly(agentId);
		assertThat(page2.getBody().get(0).supportedAgentId()).isEqualTo(agentId);
	}

	@Test
	void taskAliasRoutesBehaveLikeTasks() {
		Long agentId = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent", "Agent desc"))
				.retrieve()
				.toEntity(com.samlair.trase.agent.web.dto.AgentResponseDto.class)
				.getBody()
				.id();

		TaskResponseDto created = restClient.post()
				.uri("/task")
				.body(new CreateTaskRequestDto("Alias Task", "Alias desc", Set.of(agentId), null))
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		assertThat(created).isNotNull();

		TaskResponseDto fetched = restClient.get()
				.uri("/task/" + created.id())
				.retrieve()
				.toEntity(TaskResponseDto.class)
				.getBody();

		assertThat(fetched).isNotNull();
		assertThat(fetched.id()).isEqualTo(created.id());

		ResponseEntity<List<TaskResponseDto>> listed = restClient.get()
				.uri("/task")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});
		assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listed.getBody()).isNotEmpty();

		restClient.delete()
				.uri("/task/" + created.id())
				.retrieve()
				.toBodilessEntity();
	}

	@Test
	void getMissingTaskReturnsStandardErrorShape() throws Exception {
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.get()
				.uri("/tasks/99999")
				.retrieve()
				.toEntity(String.class));

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		ApiErrorDto error = readError(ex);
		assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(error.error()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
		assertThat(error.message()).contains("Task not found");
		assertThat(error.path()).isEqualTo("/tasks/99999");
	}

	private ApiErrorDto readError(HttpClientErrorException ex) throws Exception {
		return OBJECT_MAPPER.readValue(ex.getResponseBodyAsString(), ApiErrorDto.class);
	}
}
