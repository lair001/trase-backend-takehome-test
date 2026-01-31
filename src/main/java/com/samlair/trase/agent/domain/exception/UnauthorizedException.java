package com.samlair.trase.agent.domain.exception;

/**
 * Thrown when authentication fails.
 */
public class UnauthorizedException extends TraseAgentException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
