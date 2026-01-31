package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.IntegrationTestBase;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthControllerIntTest extends IntegrationTestBase {

	@Test
	void loginReturnsJwt() {
		RestClient rawClient = RestClient.builder()
				.baseUrl("http://localhost:" + getPort())
				.build();

		LoginResponseDto response = rawClient.post()
				.uri("/auth/login")
				.body(new LoginRequestDto("admin", "admin123!"))
				.retrieve()
				.toEntity(LoginResponseDto.class)
				.getBody();

		assertThat(response).isNotNull();
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.roles()).contains("ADMIN");
	}

	@Test
	void readerCannotCreateAgent() {
		RestClient rawClient = RestClient.builder()
				.baseUrl("http://localhost:" + getPort())
				.build();

		LoginResponseDto response = rawClient.post()
				.uri("/auth/login")
				.body(new LoginRequestDto("reader", "reader123!"))
				.retrieve()
				.toEntity(LoginResponseDto.class)
				.getBody();
		assertThat(response).isNotNull();

		RestClient readerClient = RestClient.builder()
				.baseUrl("http://localhost:" + getPort())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + response.accessToken())
				.build();

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> readerClient.post()
				.uri("/agents")
				.body(new CreateAgentRequestDto("Agent", "Desc"))
				.retrieve()
				.toEntity(String.class));

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void invalidLoginReturnsUnauthorized() {
		RestClient rawClient = RestClient.builder()
				.baseUrl("http://localhost:" + getPort())
				.build();

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> rawClient.post()
				.uri("/auth/login")
				.body(new LoginRequestDto("admin", "wrong"))
				.retrieve()
				.toEntity(String.class));

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void revokedTokenIsRejected() {
		RestClient rawClient = RestClient.builder()
				.baseUrl("http://localhost:" + getPort())
				.build();

		LoginResponseDto response = rawClient.post()
				.uri("/auth/login")
				.body(new LoginRequestDto("admin", "admin123!"))
				.retrieve()
				.toEntity(LoginResponseDto.class)
				.getBody();
		assertThat(response).isNotNull();

		RestClient authClient = RestClient.builder()
				.baseUrl("http://localhost:" + getPort())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + response.accessToken())
				.build();

		authClient.post()
				.uri("/auth/logout")
				.retrieve()
				.toBodilessEntity();

		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> authClient.get()
				.uri("/agents")
				.retrieve()
				.toEntity(String.class));

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}
}
