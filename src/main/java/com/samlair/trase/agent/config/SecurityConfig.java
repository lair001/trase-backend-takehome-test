package com.samlair.trase.agent.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures HTTP security and authorization rules.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, RevokedTokenFilter revokedTokenFilter)
			throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/login").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/swagger-ui-extra/**", "/v3/api-docs/**")
						.permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/healthz")
								.permitAll()
						.requestMatchers(HttpMethod.GET, "/audits/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/agents/**", "/tasks/**", "/task/**", "/task-runs/**")
								.hasAnyRole("ADMIN", "OPERATOR", "RUNNER", "READER")
						.requestMatchers(HttpMethod.POST, "/agents/**", "/tasks/**")
								.hasAnyRole("ADMIN", "OPERATOR")
						.requestMatchers(HttpMethod.PUT, "/agents/**", "/tasks/**")
								.hasAnyRole("ADMIN", "OPERATOR")
						.requestMatchers(HttpMethod.DELETE, "/agents/**", "/tasks/**")
								.hasAnyRole("ADMIN", "OPERATOR")
						.requestMatchers(HttpMethod.POST, "/task-runs/**")
								.hasAnyRole("ADMIN", "OPERATOR", "RUNNER")
						.requestMatchers(HttpMethod.PUT, "/task-runs/**")
								.hasAnyRole("ADMIN", "OPERATOR", "RUNNER")
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				)
				.addFilterAfter(revokedTokenFilter, BearerTokenAuthenticationFilter.class);
		return http.build();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
		return converter;
	}

	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		Object rolesClaim = jwt.getClaims().get("roles");
		if (rolesClaim instanceof Collection<?> roles) {
			return roles.stream()
					.map(Object::toString)
					.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
					.collect(Collectors.toSet());
		}
		return List.of();
	}
}
