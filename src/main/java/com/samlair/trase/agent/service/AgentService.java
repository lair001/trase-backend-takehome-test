package com.samlair.trase.agent.service;

import com.samlair.trase.agent.web.dto.CreateAgentRequestDto;
import com.samlair.trase.agent.web.dto.UpdateAgentRequestDto;
import com.samlair.trase.agent.web.dto.AgentResponseDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Business operations for managing agents.
 */
public interface AgentService {

	/**
	 * Returns all active agents.
	 *
	 * @param pageable paging parameters.
	 * @param afterId optional keyset cursor (id > afterId).
	 * @return list of agents.
	 */
	List<AgentResponseDto> listAgents(Pageable pageable, Long afterId);

	/**
	 * Creates a new agent.
	 *
	 * @param request payload describing the agent.
	 * @return created agent.
	 */
	AgentResponseDto createAgent(CreateAgentRequestDto request);

	/**
	 * Fetches a single agent by ID.
	 *
	 * @param id agent identifier.
	 * @return agent details.
	 */
	AgentResponseDto getAgent(long id);

	/**
	 * Updates an existing agent.
	 *
	 * @param id agent identifier.
	 * @param request updated fields for the agent.
	 * @return updated agent.
	 */
	AgentResponseDto updateAgent(long id, UpdateAgentRequestDto request);

	/**
	 * Soft deletes an agent.
	 *
	 * @param id agent identifier.
	 */
	void deleteAgent(long id);
}
