/**
 *
 */
package com.github.monet.common;


/**
 * This exception mainly exists to avoid the need for the try-catch bloat.
 * <p>
 * It is currently handled when the command line arguments have been parsed as
 * it is part of the validation. At runtime this exception should rarely ever
 * matter as the objects throwing this exception are singleton instances are
 * unlikely to cause trouble when they are requested again.
 */
public class DataBaseConnectionFailure extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 4958440949277340804L;

	public DataBaseConnectionFailure(String message) {
		super(message);
	}

	public DataBaseConnectionFailure(Throwable e) {
		super(e);
	}

}
