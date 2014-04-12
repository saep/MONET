/**
 *
 */
package com.github.monet.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public class VersionedPackage implements Comparable<VersionedPackage> {

	/**
	 *
	 */
	private String name;

	/**
	 *
	 */
	private BundleVersion version;

	/**
	 * This set contains package names that are provided by the worker and
	 * should not be added to the data base as dependencies.
	 */
	private static final Set<String> blacklist;

	static {
		blacklist = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
				"monet.common", "monet.interfaces", "monet.worker",
				"org.apache.logging.log4j", "org.osgi.framework",
				"monet.aggregators")));
	}

	/**
	 * @return a set of packages that are provided by the platform
	 */
	public static Set<String> packagesProvidedByMONET() {
		return blacklist;
	}

	/**
	 * Create a versioned package object by directly supplying the name of the
	 * package as well as the version.
	 * <p>
	 * If you want to create a package and ignore the version, you can provide
	 * this constructor with the <tt>getAnyVersion</tt> function in the class
	 * <tt>BundleVersion</tt>.
	 *
	 * @param name
	 *            the name of the package
	 * @param version
	 *            the version
	 * @see BundleVersion
	 */
	public VersionedPackage(String name, BundleVersion version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * Same as the default constructor, just with the possibility to fail.
	 *
	 * @param name
	 *            name of the package
	 * @param version
	 *            version string
	 * @throws BundleValidationException
	 *             if the version string could not be converted
	 * @see {@link VersionedPackage#VersionedPackage(String, String)}
	 */
	public VersionedPackage(String name, String version)
			throws BundleValidationException {
		this.name = name;
		this.version = BundleVersion.fromString(version);
	}

	@Override
	public int compareTo(VersionedPackage o) {
		return name.compareToIgnoreCase(o.getName());
	}

	/**
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the version
	 */
	public final BundleVersion getVersion() {
		return version;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 131;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof VersionedPackage)) {
			return false;
		}
		return this.compareTo((VersionedPackage) obj) == 0;
	}

	@Override
	public String toString() {
		return name + " " + version.show();
	}
}
