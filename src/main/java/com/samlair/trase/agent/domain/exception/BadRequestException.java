package com.samlair.trase.agent.domain.exception;

/**
 * Signals an invalid request or failed validation.
 */
public class BadRequestException extends TraseAgentException {

	/**
	 * Creates a bad request exception with a human-readable message.
	 */
	public BadRequestException(String message) {
		super(message);
	}
}
