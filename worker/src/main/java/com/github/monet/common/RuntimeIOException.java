package com.github.monet.common;

import java.io.IOException;

/**
 * Essentially an IOException but wrapped up in a RuntimeException to avoid
 * try-catch bloat at selected places.
 */
public class RuntimeIOException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 5077244982172435267L;

	public RuntimeIOException(IOException e) {
		super(e);
	}

}
