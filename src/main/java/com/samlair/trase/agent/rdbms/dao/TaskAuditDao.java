package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.TaskAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for task audit records.
 */
public interface TaskAuditDao extends JpaRepository<TaskAuditEntity, Long> {
}
