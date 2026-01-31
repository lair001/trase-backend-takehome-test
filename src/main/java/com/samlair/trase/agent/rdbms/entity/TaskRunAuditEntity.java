package com.samlair.trase.agent.rdbms.entity;

import com.samlair.trase.agent.domain.enumeration.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit record for task run changes.
 */
@Entity
@Table(name = "task_runs_audit")
@Getter
@Setter
@NoArgsConstructor
public class TaskRunAuditEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "task_run_id", nullable = false)
	private Long taskRunId;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false)
	private AuditAction action;

	@Column(name = "status")
	private String status;

	@Column(name = "actor_user_id")
	private Long actorUserId;

	@Column(name = "actor_username")
	private String actorUsername;

	@Column(name = "request_id")
	private String requestId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@PrePersist
	void onCreate() {
		if (occurredAt == null) {
			occurredAt = Instant.now();
		}
	}
}
