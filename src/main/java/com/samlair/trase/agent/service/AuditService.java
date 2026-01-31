package com.samlair.trase.agent.service;

import com.samlair.trase.agent.domain.enumeration.AuditAction;

/**
 * Records audit events for mutating operations.
 */
public interface AuditService {

	/**
	 * Records an audit event for agent changes.
	 *
	 * @param agentId agent identifier
	 * @param action audit action
	 */
	void recordAgentAction(Long agentId, AuditAction action);

	/**
	 * Records an audit event for task changes.
	 *
	 * @param taskId task identifier
	 * @param action audit action
	 */
	void recordTaskAction(Long taskId, AuditAction action);

	/**
	 * Records an audit event for task run changes.
	 *
	 * @param taskRunId task run identifier
	 * @param action audit action
	 * @param status task run status
	 */
	void recordTaskRunAction(Long taskRunId, AuditAction action, String status);
}
