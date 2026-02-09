package com.samlair.trase.agent;

import com.samlair.trase.agent.utils.TestDatabaseCleaner;
import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration tests for IntegrationTestBase.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("int-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.1")
			.withDatabaseName("trase_int_test")
			.withUsername("trase_int_test")
			.withPassword("trase_int_test");

	private static final String DEFAULT_USERNAME = "admin";
	private static final String DEFAULT_PASSWORD = "admin123!";

	static {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
	}

	@LocalServerPort
	private int port;

	protected RestClient restClient;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Helper for register data source properties.
	 */
	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	/**
	 * Sets up test fixtures.
	 */
	@BeforeEach
	void cleanDatabase() {
		new TestDatabaseCleaner(jdbcTemplate).clean();
		String baseUrl = "http://localhost:" + port;
		RestClient rawClient = RestClient.builder()
				.baseUrl(baseUrl)
				.build();
		LoginResponseDto auth = rawClient.post()
				.uri("/auth/login")
				.body(new LoginRequestDto(DEFAULT_USERNAME, DEFAULT_PASSWORD))
				.retrieve()
				.toEntity(LoginResponseDto.class)
				.getBody();
		String token = auth == null ? "" : auth.accessToken();
		restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.build();
	}

	/**
	 * Helper for get port.
	 */
	protected int getPort() {
		return port;
	}
}
