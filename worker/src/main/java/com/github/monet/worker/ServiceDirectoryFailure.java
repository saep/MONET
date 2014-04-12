package com.github.monet.worker;

/**
 * An exception that is thrown when the ServiceDirectory has irrevocably failed.
 *
 * @author Max Günther
 *
 */
public class ServiceDirectoryFailure extends Exception {
	private static final long serialVersionUID = 5450780213833412497L;

	public ServiceDirectoryFailure(String message) {
		super(message);
	}

	public ServiceDirectoryFailure(String message, Throwable cause) {
		super(message, cause);
	}
}
