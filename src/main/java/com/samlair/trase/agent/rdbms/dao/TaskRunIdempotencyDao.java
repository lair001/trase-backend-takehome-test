package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.TaskRunIdempotencyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for task run idempotency keys.
 */
public interface TaskRunIdempotencyDao extends JpaRepository<TaskRunIdempotencyEntity, Long> {

	Optional<TaskRunIdempotencyEntity> findByIdempotencyKey(String idempotencyKey);
}
