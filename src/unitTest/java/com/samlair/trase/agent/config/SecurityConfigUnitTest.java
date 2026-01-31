package com.samlair.trase.agent.config;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigUnitTest {

	@Test
	void extractAuthoritiesMapsRoles() throws Exception {
		SecurityConfig config = new SecurityConfig();
		Method method = SecurityConfig.class.getDeclaredMethod("extractAuthorities", Jwt.class);
		method.setAccessible(true);

		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("roles", java.util.List.of("ADMIN", "READER"))
				.build();

		@SuppressWarnings("unchecked")
		Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) method.invoke(config, jwt);
		Set<String> values = authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toSet());

		assertEquals(Set.of("ROLE_ADMIN", "ROLE_READER"), values);
	}

	@Test
	void extractAuthoritiesReturnsEmptyWhenMissing() throws Exception {
		SecurityConfig config = new SecurityConfig();
		Method method = SecurityConfig.class.getDeclaredMethod("extractAuthorities", Jwt.class);
		method.setAccessible(true);

		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("user")
				.build();

		@SuppressWarnings("unchecked")
		Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) method.invoke(config, jwt);

		assertTrue(authorities.isEmpty());
	}
}
