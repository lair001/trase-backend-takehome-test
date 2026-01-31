package com.samlair.trase.agent.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple alias for the Actuator health endpoint.
 */
@Controller
public class HealthAliasController {

	@GetMapping("/healthz")
	@Operation(summary = "Health check", description = "Alias for /actuator/health.")
	public String health() {
		return "forward:/actuator/health";
	}
}
