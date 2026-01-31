package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.service.AuditQueryService;
import com.samlair.trase.agent.web.dto.AgentAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskRunAuditResponseDto;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only audit endpoints for admins.
 */
@RestController
@RequestMapping("/audits")
@RateLimiter(name = "api")
@RequiredArgsConstructor
public class AuditController {

	private final AuditQueryService auditQueryService;

	@GetMapping("/agents")
	@Operation(
			summary = "List agent audits",
			description = "Roles: ADMIN.",
			responses = @ApiResponse(
					responseCode = "200",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									value = "[{\"id\":1,\"agentId\":10,\"action\":\"CREATE\"," +
										"\"actorUserId\":1,\"actorUsername\":\"admin\"," +
										"\"requestId\":\"req-1\",\"occurredAt\":\"2026-01-31T00:00:00Z\"}]"
							)
					)
			)
	)
	public List<AgentAuditResponseDto> listAgentAudits(
			@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return auditQueryService.listAgentAudits(pageable);
	}

	@GetMapping("/tasks")
	@Operation(
			summary = "List task audits",
			description = "Roles: ADMIN.",
			responses = @ApiResponse(
					responseCode = "200",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									value = "[{\"id\":1,\"taskId\":42,\"action\":\"UPDATE\"," +
										"\"actorUserId\":1,\"actorUsername\":\"admin\"," +
										"\"requestId\":\"req-2\",\"occurredAt\":\"2026-01-31T00:00:00Z\"}]"
							)
					)
			)
	)
	public List<TaskAuditResponseDto> listTaskAudits(
			@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return auditQueryService.listTaskAudits(pageable);
	}

	@GetMapping("/task-runs")
	@Operation(
			summary = "List task run audits",
			description = "Roles: ADMIN.",
			responses = @ApiResponse(
					responseCode = "200",
					content = @Content(
							mediaType = "application/json",
							examples = @ExampleObject(
									value = "[{\"id\":1,\"taskRunId\":99,\"action\":\"STATUS_UPDATE\"," +
										"\"status\":\"COMPLETED\",\"actorUserId\":1,\"actorUsername\":\"admin\"," +
										"\"requestId\":\"req-3\",\"occurredAt\":\"2026-01-31T00:00:00Z\"}]"
							)
					)
			)
	)
	public List<TaskRunAuditResponseDto> listTaskRunAudits(
			@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return auditQueryService.listTaskRunAudits(pageable);
	}
}
