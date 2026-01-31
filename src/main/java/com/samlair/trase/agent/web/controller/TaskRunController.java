package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.service.TaskRunService;
import com.samlair.trase.agent.web.dto.CreateTaskRunRequestDto;
import com.samlair.trase.agent.web.dto.UpdateTaskRunStatusRequestDto;
import com.samlair.trase.agent.web.dto.TaskRunResponseDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for starting and listing task runs.
 */
@RestController
@RequestMapping("/task-runs")
@RateLimiter(name = "api")
@RequiredArgsConstructor
public class TaskRunController {

	private static final Logger log = LoggerFactory.getLogger(TaskRunController.class);

	private final TaskRunService taskRunService;

	@PostMapping
	@Operation(
			summary = "Start task run",
			description = "Roles: ADMIN, OPERATOR, RUNNER. Optional Idempotency-Key header supported.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(value = "{\"taskId\":1,\"agentId\":2}")
					)
			),
			responses = @ApiResponse(
					responseCode = "201",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									value = "{\"id\":10,\"taskId\":1,\"agentId\":2,\"status\":\"RUNNING\","
											+ "\"startedAt\":\"2024-01-01T00:00:00Z\",\"completedAt\":null}"
							)
					)
			)
	)
	public ResponseEntity<TaskRunResponseDto> startTask(@Valid @RequestBody CreateTaskRunRequestDto request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
		log.debug("Start task run request received taskId={} agentId={}", request.taskId(), request.agentId());
		return ResponseEntity.status(HttpStatus.CREATED).body(taskRunService.startTaskRun(request, idempotencyKey));
	}

	@GetMapping
	@Operation(
			summary = "List task runs",
			description = "Roles: ADMIN, OPERATOR, RUNNER, READER. Example: /task-runs?status=RUNNING&afterId=100.",
			responses = @ApiResponse(
					responseCode = "200",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									name = "taskRuns",
									value = "[{\"id\":10,\"taskId\":1,\"agentId\":2,\"status\":\"RUNNING\","
											+ "\"startedAt\":\"2024-01-01T00:00:00Z\",\"completedAt\":null}]"
							)
					)
			)
	)
	@Parameters({
			@Parameter(name = "status", in = ParameterIn.QUERY, example = "RUNNING",
					description = "Optional status filter."),
			@Parameter(name = "afterId", in = ParameterIn.QUERY, example = "100",
					description = "Keyset cursor; return rows with id > afterId."),
			@Parameter(name = "page", in = ParameterIn.QUERY, example = "0",
					description = "Offset page index."),
			@Parameter(name = "size", in = ParameterIn.QUERY, example = "50",
					description = "Page size."),
			@Parameter(name = "sort", in = ParameterIn.QUERY, example = "id,asc",
					description = "Sort by property, e.g. id,asc.")
	})
	public List<TaskRunResponseDto> listTaskRuns(@RequestParam(required = false) TaskRunStatus status,
			@RequestParam(required = false) Long afterId,
			@PageableDefault(size = 50, sort = "id") Pageable pageable) {
		return taskRunService.listTaskRuns(status, pageable, afterId);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Update task run status", description = "Roles: ADMIN, OPERATOR, RUNNER.")
	public TaskRunResponseDto updateTaskRunStatus(@PathVariable long id,
			@Valid @RequestBody UpdateTaskRunStatusRequestDto request) {
		log.debug("Update task run status id={} status={}", id, request.status());
		return taskRunService.updateTaskRunStatus(id, request.status());
	}
}
