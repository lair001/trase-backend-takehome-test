package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

/**
 * Data access layer for agents.
 */
public interface AgentDao extends JpaRepository<AgentEntity, Long> {

	/**
	 * Returns all non-deleted agents.
	 *
	 * @return list of active agents.
	 */
	List<AgentEntity> findAllByDeletedAtIsNull();

	/**
	 * Returns active agents with pagination.
	 *
	 * @param pageable paging configuration.
	 * @return slice of active agents.
	 */
	Slice<AgentEntity> findAllByDeletedAtIsNull(Pageable pageable);

	/**
	 * Returns active agents with ids greater than the provided cursor.
	 *
	 * @param id lower bound for agent ids.
	 * @param pageable paging configuration.
	 * @return list of active agents.
	 */
	List<AgentEntity> findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

	/**
	 * Finds an active agent by id.
	 *
	 * @param id agent identifier.
	 * @return optional active agent.
	 */
	Optional<AgentEntity> findByIdAndDeletedAtIsNull(Long id);

	/**
	 * Finds active agents by a set of ids.
	 *
	 * @param ids agent identifiers.
	 * @return list of active agents.
	 */
	List<AgentEntity> findAllByIdInAndDeletedAtIsNull(Set<Long> ids);

	/**
	 * Checks if an active agent exists with the given name.
	 *
	 * @param name agent name.
	 * @return true when a matching agent exists.
	 */
	boolean existsByNameAndDeletedAtIsNull(String name);

	/**
	 * Checks if an active agent exists with the given name, excluding a specific id.
	 *
	 * @param name agent name.
	 * @param id agent id to exclude.
	 * @return true when a matching agent exists.
	 */
	boolean existsByNameAndDeletedAtIsNullAndIdNot(String name, Long id);
}
