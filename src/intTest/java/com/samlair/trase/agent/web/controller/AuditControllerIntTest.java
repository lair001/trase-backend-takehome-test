package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.IntegrationTestBase;
import com.samlair.trase.agent.domain.enumeration.AuditAction;
import com.samlair.trase.agent.web.dto.AgentAuditResponseDto;
import com.samlair.trase.agent.web.dto.AgentResponseDto;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.CreateTaskRunRequestDto;
import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;
import com.samlair.trase.agent.web.dto.TaskAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
import com.samlair.trase.agent.web.dto.TaskRunAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskRunResponseDto;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditControllerIntTest extends IntegrationTestBase {

	@Test
	void listAgentAuditsReturnsRecordsForAdmin() {
		AgentResponseDto agent = restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent A", "desc"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
				.getBody();
		assertThat(agent).isNotNull();

		ResponseEntity<List<AgentAuditResponseDto>> response = restClient.get()
				.uri("/audits/agents")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody())
				.anySatisfy(audit -> {
					assertThat(audit.agentId()).isEqualTo(agent.id());
					assertThat(audit.action()).isEqualTo(AuditAction.CREATE);
				});
	}

	@Test
	void listTaskAuditsReturnsRecordsForAdmin() {
		AgentResponseDto agent = createAgent("Agent A");
		TaskResponseDto task = createTask("Task A", Set.of(agent.id()));

		ResponseEntity<List<TaskAuditResponseDto>> response = restClient.get()
				.uri("/audits/tasks")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody())
				.anySatisfy(audit -> {
					assertThat(audit.taskId()).isEqualTo(task.id());
					assertThat(audit.action()).isEqualTo(AuditAction.CREATE);
				});
	}

	@Test
	void listTaskRunAuditsReturnsRecordsForAdmin() {
		AgentResponseDto agent = createAgent("Agent A");
		TaskResponseDto task = createTask("Task A", Set.of(agent.id()));

		TaskRunResponseDto run = restClient.post()
				.uri("/task-runs")
				.body(new CreateTaskRunRequestDto(task.id(), agent.id()))
				.retrieve()
				.toEntity(TaskRunResponseDto.class)
				.getBody();

		assertThat(run).isNotNull();

		ResponseEntity<List<TaskRunAuditResponseDto>> response = restClient.get()
				.uri("/audits/task-runs")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody())
				.anySatisfy(audit -> {
					assertThat(audit.taskRunId()).isEqualTo(run.id());
					assertThat(audit.action()).isEqualTo(AuditAction.START);
				});
	}

	@Test
	void readerCannotAccessAudits() {
		String baseUrl = "http://localhost:" + getPort();
		RestClient rawClient = RestClient.builder()
				.baseUrl(baseUrl)
				.build();

		LoginResponseDto auth = rawClient.post()
				.uri("/auth/login")
				.body(new LoginRequestDto("reader", "reader123!"))
				.retrieve()
				.toEntity(LoginResponseDto.class)
				.getBody();
		String token = auth == null ? "" : auth.accessToken();

		RestClient readerClient = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.build();

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> readerClient.get()
				.uri("/audits/agents")
				.retrieve()
				.toEntity(String.class));

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	private AgentResponseDto createAgent(String name) {
		return restClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto(name, "desc"))
				.retrieve()
				.toEntity(AgentResponseDto.class)
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
