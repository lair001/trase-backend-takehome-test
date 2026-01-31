package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.service.AgentService;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.UpdateAgentRequestDto;
import com.samlair.trase.agent.web.dto.AgentResponseDto;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for CRUD operations on agents.
 */
@RestController
@RequestMapping("/agents")
@RateLimiter(name = "api")
@RequiredArgsConstructor
public class AgentController {

	private static final Logger log = LoggerFactory.getLogger(AgentController.class);

	private final AgentService agentService;

	@GetMapping
	@Operation(
			summary = "List agents",
			description = "Roles: ADMIN, OPERATOR, RUNNER, READER.",
			responses = @ApiResponse(
					responseCode = "200",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									name = "agents",
									value = "[{\"id\":1,\"name\":\"Agent A\",\"description\":\"Primary agent\"}]"
							)
					)
			)
	)
	@Parameters({
			@Parameter(name = "page", in = ParameterIn.QUERY, example = "0",
					description = "Offset page index."),
			@Parameter(name = "size", in = ParameterIn.QUERY, example = "50",
					description = "Page size."),
			@Parameter(name = "sort", in = ParameterIn.QUERY, example = "id,asc",
					description = "Sort by property, e.g. id,asc.")
	})
	public List<AgentResponseDto> listAgents(
			@Parameter(description = "Keyset cursor; return rows with id > afterId.", example = "100")
			@RequestParam(required = false) Long afterId,
			@PageableDefault(size = 50, sort = "id") Pageable pageable) {
		return agentService.listAgents(pageable, afterId);
	}

	@PostMapping
	@Operation(
			summary = "Create agent",
			description = "Roles: ADMIN, OPERATOR.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(value = "{\"name\":\"Agent A\",\"description\":\"Primary agent\"}")
					)
			),
			responses = @ApiResponse(
					responseCode = "201",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									value = "{\"id\":1,\"name\":\"Agent A\",\"description\":\"Primary agent\"}"
							)
					)
			)
	)
	public ResponseEntity<AgentResponseDto> createAgent(@Valid @RequestBody CreateAgentRequestDto request) {
		log.debug("Create agent request received");
		return ResponseEntity.status(HttpStatus.CREATED).body(agentService.createAgent(request));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get agent", description = "Roles: ADMIN, OPERATOR, RUNNER, READER.")
	public AgentResponseDto getAgent(@PathVariable long id) {
		return agentService.getAgent(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update agent", description = "Roles: ADMIN, OPERATOR.")
	public AgentResponseDto updateAgent(@PathVariable long id, @Valid @RequestBody UpdateAgentRequestDto request) {
		log.debug("Update agent request received id={}", id);
		return agentService.updateAgent(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete agent", description = "Roles: ADMIN, OPERATOR.")
	public ResponseEntity<Void> deleteAgent(@PathVariable long id) {
		log.debug("Delete agent request received id={}", id);
		agentService.deleteAgent(id);
		return ResponseEntity.noContent().build();
	}
}
