package com.github.monet.common.logging;

import java.io.Serializable;

/**
 * TODO JavaDoc
 *
 * @author Marco Kuhnke
 */
public class StateEvent implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5902155869776653556L;
	private String message;

	/**
	 * Constructor.
	 *
	 * @param message
	 */
	public StateEvent(String message) {
		super();
		this.message = message;
	}

	/**
	 * Provides the message of this {@code StateEvent}.

	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		return String.format("%s", this.message);
	}

}
