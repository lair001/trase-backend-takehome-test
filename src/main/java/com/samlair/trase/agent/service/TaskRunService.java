package com.samlair.trase.agent.service;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.web.dto.CreateTaskRunRequestDto;
import com.samlair.trase.agent.web.dto.TaskRunResponseDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Business operations for starting and listing task runs.
 */
public interface TaskRunService {

	/**
	 * Starts a new task run for an agent.
	 *
	 * @param request payload describing the task run.
	 * @param idempotencyKey optional idempotency key to dedupe retries.
	 * @return created task run.
	 */
	TaskRunResponseDto startTaskRun(CreateTaskRunRequestDto request, String idempotencyKey);

	/**
	 * Lists task runs, optionally filtered by status.
	 *
	 * @param status optional status filter.
	 * @param pageable paging parameters.
	 * @param afterId optional keyset cursor (id > afterId).
	 * @return list of task runs.
	 */
	List<TaskRunResponseDto> listTaskRuns(TaskRunStatus status, Pageable pageable, Long afterId);

	/**
	 * Updates the status of an existing task run.
	 *
	 * @param id task run identifier.
	 * @param status new status.
	 * @return updated task run.
	 */
	TaskRunResponseDto updateTaskRunStatus(long id, TaskRunStatus status);
}
