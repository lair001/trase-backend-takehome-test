package com.samlair.trase.agent.service.impl;

import com.samlair.trase.agent.domain.exception.UnauthorizedException;
import com.samlair.trase.agent.rdbms.dao.UserDao;
import com.samlair.trase.agent.rdbms.entity.RoleEntity;
import com.samlair.trase.agent.rdbms.entity.UserEntity;
import com.samlair.trase.agent.service.AuthService;
import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/**
 * Issues JWTs for authenticated users.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;

	@Value("${security.jwt.issuer:trase}")
	private String issuer;

	@Value("${security.jwt.token-ttl:1h}")
	private Duration tokenTtl;

	@Override
	public LoginResponseDto login(LoginRequestDto request) {
		UserEntity user = userDao.findByUsernameAndEnabledTrue(request.username())
				.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException("Invalid credentials");
		}

		List<String> roles = user.getRoles().stream()
				.map(RoleEntity::getName)
				.sorted()
				.toList();
		Instant now = Instant.now();
		Instant expiresAt = now.plus(tokenTtl);
		String jti = UUID.randomUUID().toString();

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(now)
				.expiresAt(expiresAt)
				.subject(user.getUsername())
				.id(jti)
				.claim("uid", user.getId())
				.claim("roles", roles)
				.build();

		JwsHeader header = JwsHeader.with(() -> "RS256").build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new LoginResponseDto(token, "Bearer", expiresAt, user.getId(), roles);
	}
}
