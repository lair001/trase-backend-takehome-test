package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.exception.BadRequestException;
import com.samlair.trase.agent.domain.exception.NotFoundException;
import com.samlair.trase.agent.rdbms.dao.AgentDao;
import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.service.AuditService;
import com.samlair.trase.agent.web.dto.AgentResponseDto;
import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.UpdateAgentRequestDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplUnitTest {

	@Mock
	private AgentDao agentDao;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private AgentServiceImpl agentService;

	@Test
	void getAgentThrowsWhenMissing() {
		when(agentDao.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.empty());
		NotFoundException ex = assertThrows(NotFoundException.class, () -> agentService.getAgent(42L));
		assertTrue(ex.getMessage().contains("Agent not found"));
	}

	@Test
	void updateAgentThrowsWhenMissing() {
		when(agentDao.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.empty());
		NotFoundException ex = assertThrows(NotFoundException.class,
				() -> agentService.updateAgent(100L, new UpdateAgentRequestDto(
						"New Name", "New Description")));
		assertTrue(ex.getMessage().contains("Agent not found"));
	}

	@Test
	void createAgentRejectsDuplicateName() {
		when(agentDao.existsByNameAndDeletedAtIsNull("Agent")).thenReturn(true);

		BadRequestException ex = assertThrows(BadRequestException.class,
				() -> agentService.createAgent(new CreateAgentRequestDto("Agent", "Desc")));

		assertTrue(ex.getMessage().contains("Agent name already exists"));
		verify(agentDao, never()).save(org.mockito.ArgumentMatchers.any(AgentEntity.class));
	}

	@Test
	void deleteAgentMarksDeleted() {
		AgentEntity agent = new AgentEntity();
		agent.setId(5L);
		when(agentDao.findById(5L)).thenReturn(Optional.of(agent));

		agentService.deleteAgent(5L);

		assertNotNull(agent.getDeletedAt());
		verify(agentDao).save(agent);
	}

	@Test
	void deleteAgentIsIdempotentWhenAlreadyDeleted() {
		AgentEntity agent = new AgentEntity();
		agent.setId(6L);
		agent.setDeletedAt(Instant.parse("2024-01-01T00:00:00Z"));
		when(agentDao.findById(6L)).thenReturn(Optional.of(agent));

		agentService.deleteAgent(6L);

		verify(agentDao, never()).save(agent);
	}

	@Test
	void deleteAgentThrowsWhenMissing() {
		when(agentDao.findById(404L)).thenReturn(Optional.empty());

		NotFoundException ex = assertThrows(NotFoundException.class, () -> agentService.deleteAgent(404L));

		assertTrue(ex.getMessage().contains("Agent not found"));
	}

	@Test
	void listAgentsMapsResponse() {
		AgentEntity agent = new AgentEntity();
		agent.setId(7L);
		agent.setName("Agent");
		agent.setDescription("Desc");
		agent.setDeletedAt(null);
		agent.setSupportedTasks(java.util.Set.of());
		agent.setTaskRuns(java.util.Set.of());
		agent.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
		agent.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));
		when(agentDao.findAllByDeletedAtIsNull(Pageable.unpaged())).thenReturn(new SliceImpl<>(List.of(agent)));

		List<AgentResponseDto> response = agentService.listAgents(Pageable.unpaged(), null);

		assertEquals(1, response.size());
		assertEquals(7L, response.get(0).id());
		assertEquals("Agent", response.get(0).name());
		assertEquals("Desc", response.get(0).description());
	}

	@Test
	void getAgentReturnsResponse() {
		AgentEntity agent = new AgentEntity();
		agent.setId(3L);
		agent.setName("Agent 3");
		agent.setDescription("Desc 3");
		when(agentDao.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(agent));

		AgentResponseDto response = agentService.getAgent(3L);

		assertEquals(3L, response.id());
		assertEquals("Agent 3", response.name());
		assertEquals("Desc 3", response.description());
	}
}
