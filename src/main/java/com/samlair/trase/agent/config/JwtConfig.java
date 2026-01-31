package com.samlair.trase.agent.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Configures JWT signing and password hashing.
 */
@Configuration
public class JwtConfig {

	@Bean
	public RSAPublicKey jwtPublicKey(@Value("${security.jwt.public-key}") Resource resource) {
		return readPublicKey(resource);
	}

	@Bean
	public RSAPrivateKey jwtPrivateKey(@Value("${security.jwt.private-key}") Resource resource) {
		return readPrivateKey(resource);
	}

	@Bean
	public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
		return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}

	@Bean
	public JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
		RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private RSAPublicKey readPublicKey(Resource resource) {
		byte[] keyBytes = readPem(resource, "PUBLIC KEY");
		try {
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			return (RSAPublicKey) factory.generatePublic(spec);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to load public key", ex);
		}
	}

	private RSAPrivateKey readPrivateKey(Resource resource) {
		byte[] keyBytes = readPem(resource, "PRIVATE KEY");
		try {
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey) factory.generatePrivate(spec);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to load private key", ex);
		}
	}

	private byte[] readPem(Resource resource, String type) {
		try {
			String pem = resource.getContentAsString(StandardCharsets.US_ASCII);
			String begin = "-----BEGIN " + type + "-----";
			String end = "-----END " + type + "-----";
			String content = pem.replace(begin, "")
					.replace(end, "")
					.replaceAll("\\s", "");
			return Base64.getDecoder().decode(content);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to read key", ex);
		}
	}
}
