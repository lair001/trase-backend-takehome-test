package com.samlair.trase.agent.perf;

import com.samlair.trase.agent.utils.TestDatabaseCleaner;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("int-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerfProfileIntTest {

	private static final Logger log = LoggerFactory.getLogger(PerfProfileIntTest.class);
	private static final Path PERF_OUTPUT = Path.of("build", "perf", "perf-profile.txt");

	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.1")
			.withDatabaseName("trase_int_test")
			.withUsername("trase_int_test")
			.withPassword("trase_int_test");

	static {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
	}

	@DynamicPropertySource
	static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		new TestDatabaseCleaner(jdbcTemplate).clean();
		ensureOutputFile();
	}

	@Test
	void profileTaskAndTaskRunQueries() {
		int agentCount = intProp("perf.agents", 2000);
		int taskCount = intProp("perf.tasks", 2000);
		int agentsPerTask = intProp("perf.taskAgents", 5);
		int runCount = intProp("perf.runs", 5000);
		int pageSize = intProp("perf.pageSize", 50);
		int page = intProp("perf.page", 0);
		int offset = page * pageSize;
		int deepPage = intProp("perf.deepPage", 100);
		int deepOffset = deepPage * pageSize;
		int afterId = intProp("perf.afterId", deepOffset);

		seedAgents(agentCount);
		seedTasks(taskCount);
		seedTaskAgentLinks(taskCount, agentsPerTask, agentCount);
		seedTaskRuns(runCount, taskCount, agentCount);

		log.info("EXPLAIN agents count:");
		explain("SELECT COUNT(*) FROM agents WHERE deleted_at IS NULL");

		log.info("EXPLAIN agents page:");
		String agentPageSql = "SELECT id, name, description, created_at, updated_at FROM agents "
				+ "WHERE deleted_at IS NULL ORDER BY id LIMIT " + pageSize + " OFFSET " + offset;
		explain(agentPageSql);

		log.info("EXPLAIN agents keyset page:");
		String agentKeysetSql = "SELECT id, name, description, created_at, updated_at FROM agents "
				+ "WHERE deleted_at IS NULL AND id > " + afterId + " ORDER BY id LIMIT " + pageSize;
		explain(agentKeysetSql);

		log.info("EXPLAIN agents deep page:");
		String agentDeepPageSql = "SELECT id, name, description, created_at, updated_at FROM agents "
				+ "WHERE deleted_at IS NULL ORDER BY id LIMIT " + pageSize + " OFFSET " + deepOffset;
		explain(agentDeepPageSql);

		log.info("EXPLAIN task count:");
		explain("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL");

		log.info("EXPLAIN task page (no supported agents):");
		String taskPageSql = "SELECT t.id, t.title, t.description, t.created_at, t.updated_at "
				+ "FROM tasks t "
				+ "WHERE t.deleted_at IS NULL "
				+ "ORDER BY t.id "
				+ "LIMIT " + pageSize + " OFFSET " + offset;
		explain(taskPageSql);

		log.info("EXPLAIN task keyset page (no supported agents):");
		String taskKeysetSql = "SELECT t.id, t.title, t.description, t.created_at, t.updated_at "
				+ "FROM tasks t "
				+ "WHERE t.deleted_at IS NULL AND t.id > " + afterId + " "
				+ "ORDER BY t.id "
				+ "LIMIT " + pageSize;
		explain(taskKeysetSql);

		log.info("EXPLAIN supported agents for page task ids:");
		String taskIds = pageIds(offset, pageSize);
		String taskAgentsSql = "SELECT t.id, t.title, t.description, t.created_at, t.updated_at, "
				+ "tsa.agent_id "
				+ "FROM tasks t "
				+ "LEFT JOIN task_supported_agents tsa ON tsa.task_id = t.id "
				+ "WHERE t.deleted_at IS NULL AND t.id IN (" + taskIds + ")";
		explain(taskAgentsSql);

		log.info("EXPLAIN task page (deep offset, no supported agents):");
		String taskDeepPageSql = "SELECT t.id, t.title, t.description, t.created_at, t.updated_at "
				+ "FROM tasks t "
				+ "WHERE t.deleted_at IS NULL "
				+ "ORDER BY t.id "
				+ "LIMIT " + pageSize + " OFFSET " + deepOffset;
		explain(taskDeepPageSql);

		log.info("EXPLAIN task_run count (status filter):");
		explain("SELECT COUNT(*) FROM task_runs WHERE status = 'RUNNING'");

		log.info("EXPLAIN task_run page (status filter):");
		String runsSql = "SELECT id, task_id, agent_id, status, started_at, completed_at "
				+ "FROM task_runs WHERE status = 'RUNNING' "
				+ "ORDER BY id LIMIT " + pageSize + " OFFSET " + offset;
		explain(runsSql);

		log.info("EXPLAIN task_run keyset page (status filter):");
		String runsKeysetSql = "SELECT id, task_id, agent_id, status, started_at, completed_at "
				+ "FROM task_runs WHERE status = 'RUNNING' AND id > " + afterId + " "
				+ "ORDER BY id LIMIT " + pageSize;
		explain(runsKeysetSql);

		log.info("EXPLAIN task_run page (deep offset, status filter):");
		String runsDeepSql = "SELECT id, task_id, agent_id, status, started_at, completed_at "
				+ "FROM task_runs WHERE status = 'RUNNING' "
				+ "ORDER BY id LIMIT " + pageSize + " OFFSET " + deepOffset;
		explain(runsDeepSql);
	}

	private void seedAgents(int count) {
		List<Object[]> batch = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			batch.add(new Object[] {"agent-" + i, "desc-" + i});
		}
		jdbcTemplate.batchUpdate(
				"INSERT INTO agents (name, description, created_at, updated_at) VALUES (?, ?, now(), now())",
				batch);
	}

	private void seedTasks(int count) {
		List<Object[]> batch = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			batch.add(new Object[] {"task-" + i, "desc-" + i});
		}
		jdbcTemplate.batchUpdate(
				"INSERT INTO tasks (title, description, created_at, updated_at) VALUES (?, ?, now(), now())",
				batch);
	}

	private void seedTaskAgentLinks(int taskCount, int agentsPerTask, int agentCount) {
		List<Object[]> batch = new ArrayList<>(taskCount * agentsPerTask);
		for (int taskId = 1; taskId <= taskCount; taskId++) {
			for (int i = 0; i < agentsPerTask; i++) {
				int agentId = ((taskId + i) % agentCount) + 1;
				batch.add(new Object[] {taskId, agentId});
			}
		}
		jdbcTemplate.batchUpdate(
				"INSERT INTO task_supported_agents (task_id, agent_id) VALUES (?, ?)",
				batch);
	}

	private void seedTaskRuns(int count, int taskCount, int agentCount) {
		List<Object[]> batch = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			int taskId = (i % taskCount) + 1;
			int agentId = (i % agentCount) + 1;
			String status = (i % 2 == 0) ? "RUNNING" : "COMPLETED";
			batch.add(new Object[] {taskId, agentId, status});
		}
		jdbcTemplate.batchUpdate(
				"INSERT INTO task_runs (task_id, agent_id, status, started_at, completed_at) "
				+ "VALUES (?, ?, ?, now(), NULL)",
				batch);
	}

	private void explain(String sql) {
		List<String> planLines = jdbcTemplate.query(
				"EXPLAIN (ANALYZE, BUFFERS) " + sql,
				(rs, rowNum) -> rs.getString(1));
		appendOutput("SQL: " + sql);
		for (String line : planLines) {
			log.info(line);
			appendOutput(line);
		}
	}

	private int intProp(String key, int defaultValue) {
		String value = System.getProperty(key);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return Integer.parseInt(value);
	}

	private void ensureOutputFile() {
		try {
			Files.createDirectories(PERF_OUTPUT.getParent());
			if (!Files.exists(PERF_OUTPUT)) {
				Files.createFile(PERF_OUTPUT);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create perf output file", e);
		}
	}

	private void appendOutput(String line) {
		try {
			Files.writeString(PERF_OUTPUT, Instant.now() + " " + line + System.lineSeparator(),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to write perf output", e);
		}
	}

	private String pageIds(int offset, int pageSize) {
		int start = offset + 1;
		int end = offset + pageSize;
		StringBuilder builder = new StringBuilder();
		for (int i = start; i <= end; i++) {
			if (builder.length() > 0) {
				builder.append(',');
			}
			builder.append(i);
		}
		return builder.toString();
	}
}
