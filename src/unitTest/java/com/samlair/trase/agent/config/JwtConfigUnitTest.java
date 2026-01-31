package com.samlair.trase.agent.config;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtConfigUnitTest {

	@Test
	void loadsKeysAndBuildsJwtComponents() {
		JwtConfig config = new JwtConfig();
		RSAPublicKey publicKey = config.jwtPublicKey(new ClassPathResource("keys/dev-public.pem"));
		RSAPrivateKey privateKey = config.jwtPrivateKey(new ClassPathResource("keys/dev-private.pem"));
		JwtDecoder decoder = config.jwtDecoder(publicKey);
		JwtEncoder encoder = config.jwtEncoder(publicKey, privateKey);
		PasswordEncoder passwordEncoder = config.passwordEncoder();

		assertNotNull(publicKey);
		assertNotNull(privateKey);
		assertNotNull(decoder);
		assertNotNull(encoder);
		assertTrue(passwordEncoder.matches("pw", passwordEncoder.encode("pw")));
	}

	@Test
	void jwtPublicKeyThrowsWhenResourceMissing() {
		JwtConfig config = new JwtConfig();
		assertThrows(IllegalStateException.class,
				() -> config.jwtPublicKey(new ClassPathResource("keys/missing.pem")));
	}

	@Test
	void jwtPublicKeyThrowsWhenInvalidKey() {
		JwtConfig config = new JwtConfig();
		String pem = "-----BEGIN PUBLIC KEY-----\nYWJj\n-----END PUBLIC KEY-----";
		ByteArrayResource resource = new ByteArrayResource(pem.getBytes(StandardCharsets.US_ASCII));
		assertThrows(IllegalStateException.class, () -> config.jwtPublicKey(resource));
	}

	@Test
	void jwtPrivateKeyThrowsWhenInvalidKey() {
		JwtConfig config = new JwtConfig();
		String pem = "-----BEGIN PRIVATE KEY-----\nYWJj\n-----END PRIVATE KEY-----";
		ByteArrayResource resource = new ByteArrayResource(pem.getBytes(StandardCharsets.US_ASCII));
		assertThrows(IllegalStateException.class, () -> config.jwtPrivateKey(resource));
	}
}
