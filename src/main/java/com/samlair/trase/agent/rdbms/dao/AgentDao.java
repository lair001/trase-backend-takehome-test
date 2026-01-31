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

	List<AgentEntity> findAllByDeletedAtIsNull();

	Slice<AgentEntity> findAllByDeletedAtIsNull(Pageable pageable);

	List<AgentEntity> findAllByDeletedAtIsNullAndIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

	Optional<AgentEntity> findByIdAndDeletedAtIsNull(Long id);

	List<AgentEntity> findAllByIdInAndDeletedAtIsNull(Set<Long> ids);

	boolean existsByNameAndDeletedAtIsNull(String name);

	boolean existsByNameAndDeletedAtIsNullAndIdNot(String name, Long id);
}
