package com.samlair.trase.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class LiquibaseStartupIntTest extends IntegrationTestBase {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void liquibaseCreatesCoreTables() {
		String agentsTable = jdbcTemplate.queryForObject(
				"SELECT to_regclass('public.agents')",
				String.class
		);
		String tasksTable = jdbcTemplate.queryForObject(
				"SELECT to_regclass('public.tasks')",
				String.class
		);
		String taskRunsTable = jdbcTemplate.queryForObject(
				"SELECT to_regclass('public.task_runs')",
				String.class
		);
		assertThat(agentsTable).isEqualTo("agents");
		assertThat(tasksTable).isEqualTo("tasks");
		assertThat(taskRunsTable).isEqualTo("task_runs");

		Integer changelogCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM databasechangelog",
				Integer.class
		);
		assertThat(changelogCount).isNotNull();
		assertThat(changelogCount).isGreaterThan(0);
	}
}
