package com.samlair.trase.agent.rdbms.entity;

import com.samlair.trase.agent.domain.enumeration.TaskRunStatus;
import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.rdbms.entity.TaskEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a task run.
 */
@Entity
@Table(name = "task_runs")
@Getter
@Setter
@NoArgsConstructor
public class TaskRunEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private TaskEntity task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "agent_id", nullable = false)
	private AgentEntity agent;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TaskRunStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@PrePersist
	/**
	 * Initializes the start time for a new run if not provided.
	 */
	protected void onCreate() {
		if (startedAt == null) {
			startedAt = Instant.now();
		}
	}
}
