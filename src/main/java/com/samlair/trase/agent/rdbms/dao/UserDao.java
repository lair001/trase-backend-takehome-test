package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for users.
 */
public interface UserDao extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByUsername(String username);

	Optional<UserEntity> findByUsernameAndEnabledTrue(String username);
}
