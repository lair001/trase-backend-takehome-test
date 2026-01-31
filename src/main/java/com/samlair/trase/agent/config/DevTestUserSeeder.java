package com.samlair.trase.agent.config;

import com.samlair.trase.agent.rdbms.dao.RoleDao;
import com.samlair.trase.agent.rdbms.dao.UserDao;
import com.samlair.trase.agent.rdbms.entity.RoleEntity;
import com.samlair.trase.agent.rdbms.entity.UserEntity;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds dev/test users and roles for easy Swagger usage.
 */
@Component
@Profile({"dev", "int-test"})
@RequiredArgsConstructor
public class DevTestUserSeeder implements ApplicationRunner {

	private final UserDao userDao;
	private final RoleDao roleDao;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(ApplicationArguments args) {
		RoleEntity adminRole = ensureRole("ADMIN");
		RoleEntity operatorRole = ensureRole("OPERATOR");
		RoleEntity runnerRole = ensureRole("RUNNER");
		RoleEntity readerRole = ensureRole("READER");

		ensureUser("admin", "admin123!", Set.of(adminRole));
		ensureUser("ops", "ops123!", Set.of(operatorRole));
		ensureUser("runner", "runner123!", Set.of(runnerRole));
		ensureUser("reader", "reader123!", Set.of(readerRole));
	}

	private RoleEntity ensureRole(String name) {
		return roleDao.findByName(name).orElseGet(() -> {
			RoleEntity role = new RoleEntity();
			role.setName(name);
			return roleDao.save(role);
		});
	}

	private void ensureUser(String username, String password, Set<RoleEntity> roles) {
		if (userDao.findByUsername(username).isPresent()) {
			return;
		}
		UserEntity user = new UserEntity();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setEnabled(true);
		user.getRoles().addAll(roles);
		userDao.save(user);
	}
}
