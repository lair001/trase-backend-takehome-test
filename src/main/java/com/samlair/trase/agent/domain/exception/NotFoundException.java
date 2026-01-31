package com.samlair.trase.agent.domain.exception;

/**
 * Signals that a requested resource could not be found.
 */
public class NotFoundException extends TraseAgentException {

	/**
	 * Creates a not found exception with a human-readable message.
	 */
	public NotFoundException(String message) {
		super(message);
	}
}
