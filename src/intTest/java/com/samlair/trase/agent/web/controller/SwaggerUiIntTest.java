package com.samlair.trase.agent.web.controller;

import com.samlair.trase.agent.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerUiIntTest extends IntegrationTestBase {

	@Test
	void swaggerUiAndApiDocsArePublic() {
		RestClient rawClient = RestClient.builder()
				.baseUrl("http://localhost:" + getPort())
				.build();

		ResponseEntity<String> uiResponse = rawClient.get()
				.uri("/swagger-ui.html")
				.retrieve()
				.toEntity(String.class);
		assertThat(uiResponse.getStatusCode().is2xxSuccessful()
				|| uiResponse.getStatusCode().is3xxRedirection()).isTrue();

		ResponseEntity<String> indexResponse = rawClient.get()
				.uri("/swagger-ui/index.html")
				.retrieve()
				.toEntity(String.class);
		assertThat(indexResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(indexResponse.getBody()).contains("swagger-quick-auth");

		ResponseEntity<String> docsResponse = rawClient.get()
				.uri("/v3/api-docs")
				.retrieve()
				.toEntity(String.class);
		assertThat(docsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(docsResponse.getBody()).contains("\"openapi\"");
	}
}
