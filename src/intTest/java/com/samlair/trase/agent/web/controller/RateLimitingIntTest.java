package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.IntegrationTestBase;
import com.samlair.trase.agent.web.dto.AgentResponseDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
		"resilience4j.ratelimiter.instances.api.limit-for-period=2",
		"resilience4j.ratelimiter.instances.api.limit-refresh-period=1h",
		"resilience4j.ratelimiter.instances.api.timeout-duration=0"
})
class RateLimitingIntTest extends IntegrationTestBase {

	@Test
	void rateLimitReturns429AfterLimitExceeded() {
		ResponseEntity<List<AgentResponseDto>> first = restClient.get()
				.uri("/agents")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		ResponseEntity<List<AgentResponseDto>> second = restClient.get()
				.uri("/agents")
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {});

		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient.get()
				.uri("/agents")
				.retrieve()
				.toEntity(String.class));

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
	}
}
