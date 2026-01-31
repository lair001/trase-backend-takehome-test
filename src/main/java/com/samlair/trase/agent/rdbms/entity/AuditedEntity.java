package com.samlair.trase.agent.rdbms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Base audited entity with created and updated timestamps.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditedEntity {

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	/**
	 * Initializes timestamps when the entity is first persisted.
	 */
	protected void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	/**
	 * Updates the modified timestamp before persistence.
	 */
	protected void onUpdate() {
		updatedAt = Instant.now();
	}
}
