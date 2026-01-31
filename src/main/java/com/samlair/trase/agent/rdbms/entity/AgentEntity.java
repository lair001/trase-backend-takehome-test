package com.samlair.trase.agent.rdbms.entity;

import com.samlair.trase.agent.rdbms.entity.AuditedEntity;
import com.samlair.trase.agent.rdbms.entity.TaskEntity;
import com.samlair.trase.agent.rdbms.entity.TaskRunEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * JPA entity representing an agent.
 */
@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
public class AgentEntity extends AuditedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "description", nullable = false, columnDefinition = "text")
	private String description;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@OneToMany(mappedBy = "agent")
	private Set<TaskRunEntity> taskRuns = new HashSet<>();

	@ManyToMany(mappedBy = "supportedAgents")
	private Set<TaskEntity> supportedTasks = new HashSet<>();
}
