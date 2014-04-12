/**
 *
 */
package com.github.monet.common;

/**
 * Create an exception that is used to indicate that a bundle could not have
 * been validated properly.
 */
public class BundleValidationException extends Exception {
	/**
	 *
	 */
	private static final long serialVersionUID = 7903671920142253472L;

	/**
	 * Create the exception with the given message.
	 *
	 * @param message
	 *            the message to show
	 */
	public BundleValidationException(String message) {
		super(message);
	}

	/**
	 * Create the exception with the given message and cause.
	 *
	 * @param message
	 *            the message to show
	 * @param cause
	 *            original exception
	 */
	public BundleValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
