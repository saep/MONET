package com.github.monet.worker;

/**
 * An exception that is raised if a service could not be found locally on the
 * worker nor on the controlserver.
 *
 * @author Max Günther
 *
 */
public class ServiceNotFoundException extends Exception {
	private static final long serialVersionUID = 1073856290811272010L;

	/**
	 * Create a the exception with a message.
	 *
	 * @param message
	 *            the message to display
	 */
	public ServiceNotFoundException(String message) {
		super(message);
	}

	public ServiceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
