package com.samlair.trase.agent.service;

import com.samlair.trase.agent.web.dto.AgentAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskAuditResponseDto;
import com.samlair.trase.agent.web.dto.TaskRunAuditResponseDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Read-only access for audit records.
 */
public interface AuditQueryService {

	/**
	 * Lists agent audit records.
	 *
	 * @param pageable paging parameters.
	 * @return audit records.
	 */
	List<AgentAuditResponseDto> listAgentAudits(Pageable pageable);

	/**
	 * Lists task audit records.
	 *
	 * @param pageable paging parameters.
	 * @return audit records.
	 */
	List<TaskAuditResponseDto> listTaskAudits(Pageable pageable);

	/**
	 * Lists task run audit records.
	 *
	 * @param pageable paging parameters.
	 * @return audit records.
	 */
	List<TaskRunAuditResponseDto> listTaskRunAudits(Pageable pageable);
}
