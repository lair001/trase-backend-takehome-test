package com.samlair.trase.agent.rdbms.entity;

import com.samlair.trase.agent.rdbms.entity.AgentEntity;
import com.samlair.trase.agent.rdbms.entity.AuditedEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a task.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class TaskEntity extends AuditedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", nullable = false, columnDefinition = "text")
	private String description;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "task_supported_agents",
			joinColumns = @JoinColumn(name = "task_id"),
			inverseJoinColumns = @JoinColumn(name = "agent_id")
	)
	private Set<AgentEntity> supportedAgents = new HashSet<>();

	@OneToMany(mappedBy = "task")
	private Set<TaskRunEntity> taskRuns = new HashSet<>();
}
