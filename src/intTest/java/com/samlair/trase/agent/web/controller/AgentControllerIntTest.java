package com.samlair.trase.agent.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.samlair.trase.agent.IntegrationTestBase;
import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.web.dto.ApiErrorDto;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.AgentResponseDto;
import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
import com.samlair.trase.agent.web.dto.CreateTaskRunRequestDto;
import com.samlair.trase.agent.web.dto.TaskRunResponseDto;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.ParameterizedTypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentControllerIntTest extends IntegrationTestBase {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule());

	@Test
	void createAgentTaskAndRunLifecycle() {
		CreateAgentRequestDto agentRequest = new CreateAgentRequestDto("Agent A", "Handles tasks");
		ResponseEntity<AgentResponseDto> createdAgent = restClient.post()
				.uri("/agents")
				.body(agentRequest)
				.retrieve()
				.toEntity(AgentResponseDto.class);
		assertThat(createdAgent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		AgentResponseDto agent = createdAgent.getBody();
		assertThat(agent).isNotNull();

		CreateTaskRequestDto taskRequest = new CreateTaskRequestDto(
				"Task 1",
				"Do important work",
				Set.of(agent.id()),
				null
		);
		ResponseEntity<TaskResponseDto> createdTask = restClient.post()
				.uri("/tasks")
				.body(taskRequest)
				.retrieve()
				.toEntity(TaskResponseDto.class);
		assertThat(createdTask.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		TaskResponseDto task = createdTask.getBody();
		assertThat(task).isNotNull();
		assertThat(task.supportedAgentIds()).containsExactly(agent.id());

		CreateTaskRunRequestDto runRequest = new CreateTaskRunRequestDto(task.id(), agent.id());
		ResponseEntity<TaskRunResponseDto> createdRun = restClient.post()
				.uri("/task-runs")
				.body(runRequest)
				.retrieve()
				.toEntity(TaskRunResponseDto.class);
		assertThat(createdRun.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		TaskRunResponseDto run = createdRun.getBody();
		assertThat(run).isNotNull();
		assertThat(run.status()).isEqualTo(TaskRunStatus.RUNNING);

		ResponseEntity<List<TaskRunResponseDto>> runningRuns = restClient.get()
				.uri("/task-runs?status=RUNNING")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});
		assertThat(runningRuns.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(runningRuns.getBody()).hasSize(1);
	}

	@Test
	void createTaskRejectsUnknownAgentIds() {
		CreateTaskRequestDto taskRequest = new CreateTaskRequestDto(
				"Invalid Task",
				"Unknown agent",
				Set.of(999L),
				null
		);
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.post()
				.uri("/tasks")
				.body(taskRequest)
				.retrieve()
				.toEntity(String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createAgentValidatesInput() throws Exception {
		CreateAgentRequestDto request = new CreateAgentRequestDto("", "");
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.post()
				.uri("/agents")
				.body(request)
				.retrieve()
				.toEntity(String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		ApiErrorDto error = readError(ex);
		assertThat(error.validationErrors()).containsKeys("name", "description");
	}

	@Test
	void getAgentReturnsAgent() {
		CreateAgentRequestDto agentRequest = new CreateAgentRequestDto("Agent Fetch", "Fetchable");
		AgentResponseDto agent = restClient.post()
				.uri("/agents")
				.body(agentRequest)
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();
		assertThat(agent).isNotNull();

		AgentResponseDto fetched = restClient.get()
				.uri("/agents/" + agent.id())
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();

		assertThat(fetched).isNotNull();
		assertThat(fetched.id()).isEqualTo(agent.id());
		assertThat(fetched.name()).isEqualTo("Agent Fetch");
		assertThat(fetched.description()).isEqualTo("Fetchable");
	}

	@Test
	void updateAgentUpdatesResponse() {
		CreateAgentRequestDto agentRequest = new CreateAgentRequestDto("Agent Update", "Before");
		AgentResponseDto agent = restClient.post()
				.uri("/agents")
				.body(agentRequest)
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();
		assertThat(agent).isNotNull();

		AgentResponseDto updated = restClient.put()
				.uri("/agents/" + agent.id())
				.body(new com.samlair.trase.agent.web.dto.UpdateAgentRequestDto("Agent Update", "After"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();

		assertThat(updated).isNotNull();
		assertThat(updated.description()).isEqualTo("After");
	}

	@Test
	void deleteAgentRemovesFromQueries() {
		CreateAgentRequestDto agentRequest = new CreateAgentRequestDto("Agent A", "Handles tasks");
		AgentResponseDto agent = restClient.post()
				.uri("/agents")
				.body(agentRequest)
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();
		assertThat(agent).isNotNull();

		restClient.delete()
				.uri("/agents/" + agent.id())
				.retrieve()
				.toBodilessEntity();

		HttpClientErrorException getEx = assertThrows(HttpClientErrorException.class, () -> restClient.get()
				.uri("/agents/" + agent.id())
				.retrieve()
				.toEntity(String.class));
		assertThat(getEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<List<AgentResponseDto>> listed = restClient.get()
				.uri("/agents")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});
		assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listed.getBody()).isEmpty();
	}

	@Test
	void listAgentsRespectsPagingAndOrdering() {
		AgentResponseDto first = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent A", "First"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();
		AgentResponseDto second = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent B", "Second"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();
		AgentResponseDto third = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent C", "Third"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();

		assertThat(first).isNotNull();
		assertThat(second).isNotNull();
		assertThat(third).isNotNull();

		ResponseEntity<List<AgentResponseDto>> page0 = restClient.get()
				.uri("/agents?size=2&sort=id,asc")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(page0.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(page0.getBody()).hasSize(2);
		assertThat(page0.getBody())
				.extracting(AgentResponseDto::id)
				.containsExactly(first.id(), second.id());

		ResponseEntity<List<AgentResponseDto>> page1 = restClient.get()
				.uri("/agents?page=1&size=2&sort=id,asc")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(page1.getBody()).hasSize(1);
		assertThat(page1.getBody())
				.extracting(AgentResponseDto::id)
				.containsExactly(third.id());
	}

	@Test
	void listAgentsSupportsAfterIdKeyset() {
		AgentResponseDto first = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent A", "First"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();
		AgentResponseDto second = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent B", "Second"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();

		ResponseEntity<List<AgentResponseDto>> after = restClient.get()
				.uri("/agents?afterId=" + first.id() + "&size=10&sort=id,desc")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(after.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(after.getBody())
				.extracting(AgentResponseDto::id)
				.containsExactly(second.id());
	}

	private ApiErrorDto readError(HttpClientErrorException ex) throws Exception {
		return OBJECT_MAPPER.readValue(ex.getResponseBodyAsString(), ApiErrorDto.class);
	}
}
