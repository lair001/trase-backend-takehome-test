package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.AgentAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for agent audit records.
 */
public interface AgentAuditDao extends JpaRepository<AgentAuditEntity, Long> {
}
