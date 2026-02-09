package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.TaskRunIdempotencyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for task run idempotency keys.
 */
public interface TaskRunIdempotencyDao extends JpaRepository<TaskRunIdempotencyEntity, Long> {

	/**
	 * Finds the idempotency record by key.
	 *
	 * @param idempotencyKey idempotency key value.
	 * @return optional idempotency record.
	 */
	Optional<TaskRunIdempotencyEntity> findByIdempotencyKey(String idempotencyKey);
}
