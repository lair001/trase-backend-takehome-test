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

	@EntityGraph(attributePaths = "supportedAgents")
	@Override
	List<TaskEntity> findAll();

	@EntityGraph(attributePaths = "supportedAgents")
	@Override
	Optional<TaskEntity> findById(Long id);

	@EntityGraph(attributePaths = "supportedAgents")
	List<TaskEntity> findAllByDeletedAtIsNull();

	Slice<TaskEntity> findAllByDeletedAtIsNull(Pageable pageable);

	List<TaskEntity> findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

	@EntityGraph(attributePaths = "supportedAgents")
	Optional<TaskEntity> findByIdAndDeletedAtIsNull(Long id);

	@EntityGraph(attributePaths = "supportedAgents")
	List<TaskEntity> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

	@Query("select t from TaskEntity t where t.id = :id and t.deletedAt is null")
	Optional<TaskEntity> findByIdAndDeletedAtIsNullBasic(@Param("id") Long id);

	@Query(value = "select exists(select 1 from task_supported_agents "
			+ "where task_id = :taskId and agent_id = :agentId)", nativeQuery = true)
	boolean isAgentSupported(@Param("taskId") long taskId, @Param("agentId") long agentId);
}
