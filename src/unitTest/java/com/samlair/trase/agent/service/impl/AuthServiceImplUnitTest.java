package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.exception.UnauthorizedException;
import com.samlair.trase.agent.rdbms.dao.UserDao;
import com.samlair.trase.agent.rdbms.entity.RoleEntity;
import com.samlair.trase.agent.rdbms.entity.UserEntity;
import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplUnitTest {

	@Mock
	private UserDao userDao;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtEncoder jwtEncoder;

	@InjectMocks
	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		setField(authService, "issuer", "test-issuer");
		setField(authService, "tokenTtl", Duration.ofHours(1));
	}

	@Test
	void loginReturnsTokenAndRoles() {
		UserEntity user = new UserEntity();
		user.setId(1L);
		user.setUsername("admin");
		user.setPasswordHash("hash");
		RoleEntity admin = new RoleEntity();
		admin.setName("ADMIN");
		RoleEntity operator = new RoleEntity();
		operator.setName("OPERATOR");
		user.getRoles().addAll(Set.of(operator, admin));

		when(userDao.findByUsernameAndEnabledTrue("admin")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
		when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("admin")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.claim("roles", java.util.List.of("ADMIN", "OPERATOR"))
				.build());

		LoginResponseDto response = authService.login(new LoginRequestDto("admin", "pw"));

		assertEquals("token", response.accessToken());
		assertEquals("Bearer", response.tokenType());
		assertEquals(java.util.List.of("ADMIN", "OPERATOR"), response.roles());
	}

	@Test
	void loginRejectsInvalidPassword() {
		UserEntity user = new UserEntity();
		user.setUsername("admin");
		user.setPasswordHash("hash");

		when(userDao.findByUsernameAndEnabledTrue("admin")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

		assertThrows(UnauthorizedException.class, () -> authService.login(new LoginRequestDto("admin", "bad")));
	}

	@Test
	void loginRejectsUnknownUser() {
		when(userDao.findByUsernameAndEnabledTrue("missing")).thenReturn(Optional.empty());

		assertThrows(UnauthorizedException.class, () -> authService.login(new LoginRequestDto("missing", "pw")));
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to set field " + fieldName, ex);
		}
	}
}
