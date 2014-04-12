package com.github.monet.controlserver;

import java.io.File;
import java.io.InputStream;

import com.github.monet.common.BundleValidationException;

/**
 * This class sets the required fields for the ParameterList class.
 *
 * @see ParameterList
 */
public class AlgorithmParameterList extends ParameterList {

	/**
	 *
	 */
	private static final long serialVersionUID = -4868176042145107954L;

	public AlgorithmParameterList(String bundleDescriptorString)
			throws BundleValidationException {
		super(new AlgorithmParameterIterator(bundleDescriptorString));
	}

	/**
	 * This package local constructor is only used for testing.
	 *
	 * @param xmlis
	 * @throws BundleValidationException
	 */
	AlgorithmParameterList(InputStream xmlis) throws BundleValidationException {
		super(new AlgorithmParameterIterator(xmlis));
	}

	@Override
	protected ParameterIterator createIteratorInstance(File xml)
			throws BundleValidationException {
		return new AlgorithmParameterIterator(xml);
	}

}
