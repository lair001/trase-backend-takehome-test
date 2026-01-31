package com.samlair.trase.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class BootJarSmokeIntTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.1")
			.withDatabaseName("trase")
			.withUsername("trase")
			.withPassword("trase");

	private static HttpClient httpClient;

	@BeforeAll
	static void startDependencies() {
		httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(2))
				.build();
	}

	@AfterAll
	static void stopDependencies() {
		POSTGRES.stop();
	}

	@Test
	void bootJarStartsAndRunsMigrations() throws Exception {
		int port = findFreePort();
		Path jarPath = findBootJar();
		Process process = startBootJar(jarPath, port);

		StringBuilder output = new StringBuilder();
		Thread logThread = streamLogs(process, output);
		try {
			waitForHealthy(port, output);
			assertTablesExist();
		} finally {
			process.destroy();
			process.waitFor(10, TimeUnit.SECONDS);
			process.destroyForcibly();
			logThread.join(1000);
		}
	}

	private static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static Path findBootJar() throws IOException {
		Path libsDir = Path.of("build", "libs");
		try (var stream = Files.list(libsDir)) {
			return stream
					.filter(path -> path.getFileName().toString().endsWith(".jar"))
					.max(Comparator.comparingLong(path -> path.toFile().lastModified()))
					.orElseThrow(() -> new IllegalStateException("No boot jar found in build/libs"));
		}
	}

	private static Process startBootJar(Path jarPath, int port) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(List.of(
				"java",
				"-jar",
				jarPath.toString()
		));
		builder.environment().put("PORT", String.valueOf(port));
		builder.environment().put("DB_HOST", POSTGRES.getHost());
		builder.environment().put("DB_PORT", String.valueOf(POSTGRES.getFirstMappedPort()));
		builder.environment().put("DB_NAME", "trase");
		builder.environment().put("DB_USER", POSTGRES.getUsername());
		builder.environment().put("DB_PASSWORD", POSTGRES.getPassword());
		builder.redirectErrorStream(true);
		return builder.start();
	}

	private static Thread streamLogs(Process process, StringBuilder output) {
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append(System.lineSeparator());
				}
			} catch (IOException ignored) {
				// Ignore stream close issues during shutdown.
			}
		});
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	private static void waitForHealthy(int port, StringBuilder output) throws Exception {
		URI uri = URI.create("http://localhost:" + port + "/actuator/health");
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(45);
		while (System.currentTimeMillis() < deadline) {
			try {
				HttpRequest request = HttpRequest.newBuilder(uri)
						.timeout(Duration.ofSeconds(2))
						.GET()
						.build();
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"")) {
					return;
				}
			} catch (IOException | InterruptedException ignored) {
				// Retry until deadline.
			}
			Thread.sleep(500);
		}
		throw new IllegalStateException("Boot jar did not become healthy:\n" + output);
	}

	private static void assertTablesExist() throws Exception {
		try (var connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(),
				POSTGRES.getUsername(),
				POSTGRES.getPassword());
			 var statement = connection.createStatement()) {
			assertThat(exists(statement, "public.agents")).isTrue();
			assertThat(exists(statement, "public.tasks")).isTrue();
			assertThat(exists(statement, "public.task_runs")).isTrue();
			assertThat(changelogCount(statement)).isGreaterThan(0);
		}
	}

	private static boolean exists(java.sql.Statement statement, String table) throws Exception {
		try (var result = statement.executeQuery("SELECT to_regclass('" + table + "')")) {
			return result.next() && result.getString(1) != null;
		}
	}

	private static int changelogCount(java.sql.Statement statement) throws Exception {
		try (var result = statement.executeQuery("SELECT COUNT(*) FROM databasechangelog")) {
			result.next();
			return result.getInt(1);
		}
	}
}
