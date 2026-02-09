package com.samlair.trase.agent.web.dto;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for UpdateTaskRequestDtoUnitTest.
 */
class UpdateTaskRequestDtoUnitTest {

	/**
	 * Verifies supports agent ids.
	 */
	@Test
	void supportsAgentIds() {
		UpdateTaskRequestDto dto = new UpdateTaskRequestDto("Title", "Desc", Set.of(1L), null);
		assertTrue(dto.isSupportedAgentValid());
	}

	/**
	 * Verifies supports single agent id.
	 */
	@Test
	void supportsSingleAgentId() {
		UpdateTaskRequestDto dto = new UpdateTaskRequestDto("Title", "Desc", null, 2L);
		assertTrue(dto.isSupportedAgentValid());
	}

	/**
	 * Verifies rejects missing agents.
	 */
	@Test
	void rejectsMissingAgents() {
		UpdateTaskRequestDto dto = new UpdateTaskRequestDto("Title", "Desc", null, null);
		assertFalse(dto.isSupportedAgentValid());
	}

	/**
	 * Verifies rejects empty agent set.
	 */
	@Test
	void rejectsEmptyAgentSet() {
		UpdateTaskRequestDto dto = new UpdateTaskRequestDto("Title", "Desc", Set.of(), null);
		assertFalse(dto.isSupportedAgentValid());
	}
}
