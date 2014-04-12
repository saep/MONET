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
public class ParserParameterIterator extends ParameterIterator {
	private static final long serialVersionUID = -9160885389985519510L;

	/**
	 * Create an ParserParameterIterator from the given
	 * bundleDescriptorString.
	 * <p>
	 * The bundle is retrieved from the data base, validated and then the
	 * iterator is created.
	 *
	 * @param bundleDescriptorString
	 *            the bundle descriptor string
	 * @throws ValidationException
	 */
	public ParserParameterIterator(String bundleDescriptorString)
			throws BundleValidationException {
		super(GRAPH_ROOT_ELEMENT, bundleDescriptorString);

	}

	/**
	 * Create an ParserParameterIterator directly from a xml description
	 * file.
	 *
	 * @param xml
	 *            the XML file as an InputStream
	 * @throws BundleValidationException
	 */
	public ParserParameterIterator(InputStream xml)
			throws BundleValidationException {
		super(GRAPH_ROOT_ELEMENT, xml);
	}

	/**
	 * Create a bundle iterator from a given ZIP/JAR/XML file.
	 *
	 * @param bundleFile
	 *            the bundles JAR file
	 * @throws BundleValidationException
	 */
	public ParserParameterIterator(File bundleFile)
			throws BundleValidationException {
		super(GRAPH_ROOT_ELEMENT, bundleFile);
	}

}
