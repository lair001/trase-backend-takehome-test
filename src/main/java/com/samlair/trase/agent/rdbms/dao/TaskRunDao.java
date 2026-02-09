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
	/**
	 * Finds task runs with the given status.
	 *
	 * @param status run status filter.
	 * @return list of matching task runs.
	 */
	List<TaskRunEntity> findByStatus(TaskRunStatus status);

	/**
	 * Finds task runs with the given status and pagination.
	 *
	 * @param status run status filter.
	 * @param pageable paging configuration.
	 * @return slice of matching task runs.
	 */
	Slice<TaskRunEntity> findByStatus(TaskRunStatus status, Pageable pageable);

	/**
	 * Lists task runs with pagination.
	 *
	 * @param pageable paging configuration.
	 * @return slice of task runs.
	 */
	Slice<TaskRunEntity> findAllBy(Pageable pageable);

	/**
	 * Lists task runs with ids greater than the provided cursor.
	 *
	 * @param id lower bound for task run ids.
	 * @param pageable paging configuration.
	 * @return list of task runs.
	 */
	List<TaskRunEntity> findAllByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

	/**
	 * Lists task runs by status with ids greater than the provided cursor.
	 *
	 * @param status run status filter.
	 * @param id lower bound for task run ids.
	 * @param pageable paging configuration.
	 * @return list of task runs.
	 */
	List<TaskRunEntity> findByStatusAndIdGreaterThanOrderByIdAsc(TaskRunStatus status, Long id, Pageable pageable);
}
