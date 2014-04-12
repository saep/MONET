/**
 *
 */
package com.github.monet.controlserver;

import java.io.File;
import java.io.InputStream;

import javax.xml.bind.ValidationException;

import com.github.monet.common.BundleValidationException;

/**
 * @see ParameterIterator
 */
public class AlgorithmParameterIterator extends ParameterIterator {

	/**
	 *
	 */
	private static final long serialVersionUID = -9160885389985519510L;

	/**
	 * Create an AlgorithmParameterIterator from the given
	 * bundleDescriptorString.
	 * <p>
	 * The bundle is retrieved from the data base, validated and then the
	 * iterator is created.
	 *
	 * @param bundleDescriptorString
	 *            the bundle descriptor string
	 * @throws ValidationException
	 */
	public AlgorithmParameterIterator(String bundleDescriptorString)
			throws BundleValidationException {
		super(ALGORITHM_ROOT_ELEMENT, bundleDescriptorString);

	}

	/**
	 * Create an AlgorithmParameterIterator directly from a xml description
	 * file.
	 *
	 * @param xml
	 *            the XML file as an InputStream
	 * @throws BundleValidationException
	 */
	public AlgorithmParameterIterator(InputStream xml)
			throws BundleValidationException {
		super(ALGORITHM_ROOT_ELEMENT, xml);
	}

	/**
	 * Create a bundle iterator from a given ZIP/JAR/XML file.
	 *
	 * @param bundleFile
	 *            the bundles JAR file
	 * @throws BundleValidationException
	 */
	public AlgorithmParameterIterator(File bundleFile)
			throws BundleValidationException {
		super(ALGORITHM_ROOT_ELEMENT, bundleFile);
	}

}
