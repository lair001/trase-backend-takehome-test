package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for roles.
 */
public interface RoleDao extends JpaRepository<RoleEntity, Long> {

	Optional<RoleEntity> findByName(String name);
}
