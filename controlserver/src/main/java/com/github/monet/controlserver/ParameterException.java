/**
 *
 */
package com.github.monet.controlserver;

import java.util.List;

/**
 * This exception is thrown when the AlgorithmParameterIterator could not parse
 * one or more values.
 */
public class ParameterException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 3757020728279397002L;

	/**
	 * The list of faulty parameters.
	 */
	private List<Parameter> faultyParameters;

	/**
	 * Create an ParameterException object by supplying the list of fault set
	 * parameters.
	 *
	 * @param faultyParameters
	 *            the list of parameters that were not set properly
	 */
	public ParameterException(List<Parameter> faultyParameters) {
		this.faultyParameters = faultyParameters;
	}

	/**
	 * @return the map of faulty parameters
	 */
	public List<Parameter> getFaultyParameters() {
		return faultyParameters;
	}

}
