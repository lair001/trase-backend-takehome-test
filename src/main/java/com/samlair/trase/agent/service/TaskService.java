package com.samlair.trase.agent.service;

import com.samlair.trase.agent.web.dto.CreateTaskRequestDto;
import com.samlair.trase.agent.web.dto.UpdateTaskRequestDto;
import com.samlair.trase.agent.web.dto.TaskResponseDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Business operations for managing tasks.
 */
public interface TaskService {

	/**
	 * Returns all active tasks.
	 *
	 * @param pageable paging parameters.
	 * @param afterId optional keyset cursor (id > afterId).
	 * @return list of tasks.
	 */
	List<TaskResponseDto> listTasks(Pageable pageable, Long afterId);

	/**
	 * Creates a new task.
	 *
	 * @param request payload describing the task.
	 * @return created task.
	 */
	TaskResponseDto createTask(CreateTaskRequestDto request);

	/**
	 * Fetches a single task by ID.
	 *
	 * @param id task identifier.
	 * @return task details.
	 */
	TaskResponseDto getTask(long id);

	/**
	 * Updates an existing task.
	 *
	 * @param id task identifier.
	 * @param request updated fields for the task.
	 * @return updated task.
	 */
	TaskResponseDto updateTask(long id, UpdateTaskRequestDto request);

	/**
	 * Soft deletes a task.
	 *
	 * @param id task identifier.
	 */
	void deleteTask(long id);
}
