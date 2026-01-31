package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.rdbms.entity.TaskRunEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

/**
 * Data access layer for task runs.
 */
public interface TaskRunDao extends JpaRepository<TaskRunEntity, Long> {
	List<TaskRunEntity> findByStatus(TaskRunStatus status);

	Slice<TaskRunEntity> findByStatus(TaskRunStatus status, Pageable pageable);

	Slice<TaskRunEntity> findAllBy(Pageable pageable);

	List<TaskRunEntity> findAllByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

	List<TaskRunEntity> findByStatusAndIdGreaterThanOrderByIdAsc(TaskRunStatus status, Long id, Pageable pageable);
}
