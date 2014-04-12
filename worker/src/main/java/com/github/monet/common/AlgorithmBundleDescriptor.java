/**
 *
 */
package com.github.monet.common;

import java.io.File;

/**
 * A specialized version of the bundle descriptor which stores algorithm
 * specific information.
 */
public class AlgorithmBundleDescriptor extends BundleDescriptor {

	/**
	 *
	 */
	private static final long serialVersionUID = 1320669762903694735L;

	/**
	 * The type of the algorithm. (e.g. SSSP, MST)
	 */
	private String algorithmType;

	/**
	 * The arity of the algorithm. Or in other words, how many objectives the
	 * algorithm can handle.
	 */
	private int arity;

	/**
	 * Create a bundle descriptor for the given attributes.
	 * <p>
	 * Usually it is more convenient to call the static method
	 * <tt>fromFile(..)</tt> in * the super class.
	 *
	 * @param bundleName
	 *            the bundle's name
	 * @param version
	 *            the version of the bundle
	 * @param bundleFile
	 *            the file of the bundle
	 * @param algorithmType
	 *            the type of the algorithm
	 * @param arity
	 *            the arity of the algorithm
	 * @throws BundleValidationException
	 *             if any of the parameters do not conform to the specification
	 * @see BundleDescriptor
	 */
	public AlgorithmBundleDescriptor(String bundleName, String version,
			File bundleFile, String algorithmType, int arity)
			throws BundleValidationException {
		super(bundleName, version, bundleFile);
		this.arity = arity;
		this.algorithmType = algorithmType;
	}

	/**
	 * Any value smaller than 1 means that this algorithm can handle any arity.
	 *
	 * @return the arity of the algorithm
	 */
	public int getArity() {
		return arity;
	}

	@Override
	public Kind kind() {
		return Kind.ALGORITHM;
	}

	/**
	 * @return the type of the algorithm
	 */
	public String getAlgorithmType() {
		return algorithmType;
	}

}
