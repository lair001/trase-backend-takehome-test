package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.service.TaskService;
import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.UpdateTaskRequestDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
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
 * REST endpoints for CRUD operations on tasks.
 */
@RestController
@RequestMapping({"/tasks", "/task"})
@RateLimiter(name = "api")
@RequiredArgsConstructor
public class TaskController {

	private static final Logger log = LoggerFactory.getLogger(TaskController.class);

	private final TaskService taskService;

	@GetMapping
	@Operation(
			summary = "List tasks",
			description = "Roles: ADMIN, OPERATOR, RUNNER, READER.",
			responses = @ApiResponse(
					responseCode = "200",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									name = "tasks",
									value = "[{\"id\":1,\"title\":\"Task A\",\"description\":\"Primary task\",\"supportedAgentIds\":[1,2]}]"
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
	public List<TaskResponseDto> listTasks(
			@Parameter(description = "Keyset cursor; return rows with id > afterId.", example = "100")
			@RequestParam(required = false) Long afterId,
			@PageableDefault(size = 50, sort = "id") Pageable pageable) {
		return taskService.listTasks(pageable, afterId);
	}

	@PostMapping
	@Operation(
			summary = "Create task",
			description = "Roles: ADMIN, OPERATOR.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									value = "{\"title\":\"Task A\",\"description\":\"Primary task\",\"supportedAgentIds\":[1,2]}"
							)
					)
			),
			responses = @ApiResponse(
					responseCode = "201",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									value = "{\"id\":1,\"title\":\"Task A\",\"description\":\"Primary task\",\"supportedAgentIds\":[1,2]}"
							)
					)
			)
	)
	public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody CreateTaskRequestDto request) {
		log.debug("Create task request received");
		return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get task", description = "Roles: ADMIN, OPERATOR, RUNNER, READER.")
	public TaskResponseDto getTask(@PathVariable long id) {
		return taskService.getTask(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update task", description = "Roles: ADMIN, OPERATOR.")
	public TaskResponseDto updateTask(@PathVariable long id, @Valid @RequestBody UpdateTaskRequestDto request) {
		log.debug("Update task request received id={}", id);
		return taskService.updateTask(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete task", description = "Roles: ADMIN, OPERATOR.")
	public ResponseEntity<Void> deleteTask(@PathVariable long id) {
		log.debug("Delete task request received id={}", id);
		taskService.deleteTask(id);
		return ResponseEntity.noContent().build();
	}
}
