package com.github.monet.worker;

/**
 * An exception that is raised when some kind of error occurs with the database.
 *
 * @author Max Günther
 *
 */
public class DatabaseFailure extends Exception {
	private static final long serialVersionUID = -3180274350352675667L;

	public DatabaseFailure(String message) {
		super(message);
	}

	public DatabaseFailure(String message, Throwable cause) {
		super(message, cause);
	}
}
