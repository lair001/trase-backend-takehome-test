package com.samlair.trase.agent.rdbms.dao;

import com.samlair.trase.agent.rdbms.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access layer for users.
 */
public interface UserDao extends JpaRepository<UserEntity, Long> {

	/**
	 * Finds a user by username.
	 *
	 * @param username username to search.
	 * @return optional user.
	 */
	Optional<UserEntity> findByUsername(String username);

	/**
	 * Finds an enabled user by username.
	 *
	 * @param username username to search.
	 * @return optional enabled user.
	 */
	Optional<UserEntity> findByUsernameAndEnabledTrue(String username);
}
