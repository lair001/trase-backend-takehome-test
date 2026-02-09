package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.TaskEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access layer for tasks.
 */
public interface TaskDao extends JpaRepository<TaskEntity, Long> {

	/**
	 * {@inheritDoc}
	 */
	@EntityGraph(attributePaths = "supportedAgents")
	@Override
	List<TaskEntity> findAll();

	/**
	 * {@inheritDoc}
	 */
	@EntityGraph(attributePaths = "supportedAgents")
	@Override
	Optional<TaskEntity> findById(Long id);

	/**
	 * Returns all non-deleted tasks with supported agents loaded.
	 *
	 * @return list of active tasks.
	 */
	@EntityGraph(attributePaths = "supportedAgents")
	List<TaskEntity> findAllByDeletedAtIsNull();

	/**
	 * Returns active tasks with pagination.
	 *
	 * @param pageable paging configuration.
	 * @return slice of active tasks.
	 */
	Slice<TaskEntity> findAllByDeletedAtIsNull(Pageable pageable);

	/**
	 * Returns active tasks with ids greater than the provided cursor.
	 *
	 * @param id lower bound for task ids.
	 * @param pageable paging configuration.
	 * @return list of active tasks.
	 */
	List<TaskEntity> findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

	/**
	 * Finds an active task by id with supported agents loaded.
	 *
	 * @param id task identifier.
	 * @return optional active task.
	 */
	@EntityGraph(attributePaths = "supportedAgents")
	Optional<TaskEntity> findByIdAndDeletedAtIsNull(Long id);

	/**
	 * Finds active tasks by id with supported agents loaded.
	 *
	 * @param ids task identifiers.
	 * @return list of active tasks.
	 */
	@EntityGraph(attributePaths = "supportedAgents")
	List<TaskEntity> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

	/**
	 * Finds an active task by id without fetching relationships.
	 *
	 * @param id task identifier.
	 * @return optional active task.
	 */
	@Query("select t from TaskEntity t where t.id = :id and t.deletedAt is null")
	Optional<TaskEntity> findByIdAndDeletedAtIsNullBasic(@Param("id") Long id);

	/**
	 * Checks if the agent is supported by a task.
	 *
	 * @param taskId task identifier.
	 * @param agentId agent identifier.
	 * @return true if the agent is supported.
	 */
	@Query(value = "select exists(select 1 from task_supported_agents "
			+ "where task_id = :taskId and agent_id = :agentId)", nativeQuery = true)
	boolean isAgentSupported(@Param("taskId") long taskId, @Param("agentId") long agentId);
}
