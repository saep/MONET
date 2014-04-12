package com.github.monet.controlserver;

import java.io.File;
import java.io.InputStream;

import com.github.monet.common.BundleValidationException;

/**
 * This class sets the required fields for the ParameterList class.
 *
 * @see ParameterList
 */
public class ParserParameterList extends ParameterList {
	private static final long serialVersionUID = -4868176042145107954L;

	public ParserParameterList(String bundleDescriptorString)
			throws BundleValidationException {
		super(new ParserParameterIterator(bundleDescriptorString));
	}

	/**
	 * This package local constructor is only used for testing.
	 *
	 * @param xmlis
	 * @throws BundleValidationException
	 */
	ParserParameterList(InputStream xmlis) throws BundleValidationException {
		super(new ParserParameterIterator(xmlis));
	}

	@Override
	protected ParameterIterator createIteratorInstance(File xml)
			throws BundleValidationException {
		return new ParserParameterIterator(xml);
	}

}
