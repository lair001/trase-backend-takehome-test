package com.samlair.trase.agent.domain.exception;

/**
 * Base runtime exception for domain and API errors.
 */
public class TraseAgentException extends RuntimeException {

	/**
	 * Creates a base exception with a human-readable message.
	 */
	public TraseAgentException(String message) {
		super(message);
	}
}
