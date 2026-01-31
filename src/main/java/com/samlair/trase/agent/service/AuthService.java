package com.samlair.trase.agent.service;

import com.samlair.trase.agent.web.dto.LoginRequestDto;
import com.samlair.trase.agent.web.dto.LoginResponseDto;

/**
 * Authentication operations for issuing JWTs.
 */
public interface AuthService {

	/**
	 * Authenticates the user and returns an access token.
	 *
	 * @param request login credentials
	 * @return login response with JWT and roles
	 */
	LoginResponseDto login(LoginRequestDto request);
}
