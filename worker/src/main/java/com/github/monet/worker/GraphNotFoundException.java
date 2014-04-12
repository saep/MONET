package com.github.monet.worker;

public class GraphNotFoundException extends Exception{

	/**
	 *
	 */
	private static final long serialVersionUID = -6110645056786564651L;

	/**
	 * Create a the exception with a message.
	 *
	 * @param message
	 *            the message to display
	 */
	public GraphNotFoundException(String message) {
		super(message);
	}

	public GraphNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
