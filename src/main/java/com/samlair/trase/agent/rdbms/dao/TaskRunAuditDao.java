package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.TaskRunAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for task run audit records.
 */
public interface TaskRunAuditDao extends JpaRepository<TaskRunAuditEntity, Long> {
}
