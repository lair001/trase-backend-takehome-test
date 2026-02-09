package com.samlair.trase.agent.utils;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test utilities for TestDatabaseCleaner.
 */
public class TestDatabaseCleaner {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Helper for test database cleaner.
	 */
	public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Helper for clean.
	 */
	public void clean() {
		String exists = jdbcTemplate.queryForObject(
				"SELECT to_regclass('public.task_runs')",
				String.class
		);
		if (exists == null) {
			return;
		}
		jdbcTemplate.execute(
				"TRUNCATE TABLE revoked_tokens, task_runs_audit, tasks_audit, agents_audit, "
						+ "task_runs, task_supported_agents, tasks, agents RESTART IDENTITY CASCADE"
		);
	}
}
