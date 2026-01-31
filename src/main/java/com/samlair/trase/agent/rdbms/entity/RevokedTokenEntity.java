package com.samlair.trase.agent.rdbms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * JPA entity representing a revoked JWT.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RevokedTokenEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "jti", nullable = false, unique = true)
	private String jti;

	@Column(name = "revoked_at", nullable = false)
	private Instant revokedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@PrePersist
	void onCreate() {
		if (revokedAt == null) {
			revokedAt = Instant.now();
		}
	}
}
