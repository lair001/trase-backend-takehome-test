package com.samlair.trase.agent.domain.exception;

/**
 * Thrown when authentication fails.
 */
public class UnauthorizedException extends TraseAgentException {

	/**
	 * Creates an unauthorized exception with a human-readable message.
	 */
	public UnauthorizedException(String message) {
		super(message);
	}
}
