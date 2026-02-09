package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for HealthAliasControllerIntTest.
 */
class HealthAliasControllerIntTest extends IntegrationTestBase {

	/**
	 * Verifies healthz returns ok.
	 */
	@Test
	void healthzReturnsOk() {
		ResponseEntity<String> response = restClient.get()
				.uri("/healthz")
				.retrieve()
				.toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"status\":\"UP\"");
	}
}
