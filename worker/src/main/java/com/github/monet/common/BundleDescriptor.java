package com.github.monet.common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Convenience class to handle bundle descriptors.
 * <p>
 * These are extremely useful for dependency management and bundle clean up.
 *
 */
public class BundleDescriptor extends VersionedPackage implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 7210033833596605908L;

	private static Logger LOG = LogManager.getLogger(BundleDescriptor.class);

	/**
	 *
	 */
	private static Pattern NAMEFORMAT = Pattern.compile("[-_A-Za-z0-9]+");
	/**
	 * File in which the bundle is stored. May be null!
	 */
	private File file;

	/**
	 * Enumeration describing the kind of algorithm descriptor.
	 */
	public enum Kind {
		/**
		 * A generic bundle descriptor. This one can be anything!
		 */
		GENERIC,
		/**
		 * A bundle descriptor for algorithms.
		 */
		ALGORITHM,
		/**
		 * A parser bundle descriptor.
		 */
		PARSER,
		/**
		 * A graph bundle descriptor.
		 */
		GRAPH
	};

	/**
	 * @return the kind of bundle descriptor
	 */
	public Kind kind() {
		return Kind.GENERIC;
	}

	/**
	 * Initialize the BundleDescriptor object using the descriptor string
	 * directly.
	 * <p>
	 * The descriptor must look like:
	 * <tt>exampleBundleName#1.33.7-OPTIONALVERSIONDESCRIPTION</tt>
	 * <p>
	 * The version description is typically something like <tt>SNAPSHOT</tt> or
	 * <tt>RELEASE</tt>.
	 * <p>
	 * Note that this function does not check whether the descriptor string has
	 * a valid format as the resulting exception handling would cause for too
	 * much overhead in the usage of this function. If a check is required, use
	 * the static function <tt>isValid(bundleDescriptor)</tt> from this class.
	 *
	 * @param descriptor
	 *            the descriptor string
	 * @throws BundleValidationException
	 */
	public BundleDescriptor(String descriptor) throws BundleValidationException {
		this(descriptor, (File) null);
	}

	/**
	 * Create a BundleDescriptor object for the given bundle descriptor string
	 * and file.
	 *
	 * @param descriptor
	 *            the descriptor string
	 * @param file
	 *            the file of the bundle
	 * @throws BundleValidationException
	 */
	public BundleDescriptor(String descriptor, File file)
			throws BundleValidationException {
		this(descriptor.substring(0, descriptor.lastIndexOf('#')), descriptor
				.substring(descriptor.lastIndexOf('#') + 1), file);
	}

	/**
	 * Create a BundleDescriptor object for the given bundle name and version.
	 * <p>
	 * This works in the opposite direction than the other constructor.
	 *
	 * @param bundleName
	 * @param version
	 * @throws BundleValidationException
	 */
	public BundleDescriptor(String bundleName, String version)
			throws BundleValidationException {
		this(bundleName, version, null);
	}

	public BundleDescriptor(String bundleName, String version, File bundleFile)
			throws BundleValidationException {
		super(bundleName, BundleVersion.fromString(version));
		if (getVersion().kind() != BundleVersion.Kind.SINGLE) {
			throw new BundleValidationException(
					"It does not make any sense to give a version range as a "
							+ "version for a single bundle!");
		}
		this.file = null;
	}

	/**
	 *
	 * @return the bundle descriptor
	 */
	public String getDescriptor() {
		return getName() + "#" + getVersion();
	}

	/**
	 * Returns a standardized name for the .jar. This might be different from
	 * {@link #getFile()}.
	 *
	 * @return the corresponding jar name of the bundle descriptor
	 */
	public String getCleanJarName() {
		return (getName() + "-" + getVersion().show() + ".jar");
	}

	/**
	 * Returns the bundle file for the BundleDescriptor. This might be null if
	 * hasn't been set in the constructor.
	 *
	 * @return the .jar file of the bundle
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * Set the file the bundle. Used by the ServiceDirectory once a file has
	 * been retrieved from the controlserver.
	 *
	 * @param file
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * Check whether the given bundle name is valid.
	 *
	 * @return true if the bundle name is valid
	 */
	public static boolean isValidName(final String name) {
		return NAMEFORMAT.matcher(name).matches();
	}

	/**
	 * Check whether the given bundle version string is valid.
	 *
	 * @return true if the version string is valid
	 */
	public static boolean isValidVersion(final String version) {
		return BundleVersion.VERSIONFORMAT.matcher(version).matches();
	}

	/**
	 * Check whether a given bundle descriptor's syntax is valid.
	 *
	 * @param descriptor
	 *            the bundle descriptor
	 * @return indicator whether the descriptor is valid
	 */
	public static boolean isValid(final String descriptor) {
		if (descriptor == null) {
			return false;
		}
		int i = descriptor.lastIndexOf('#');
		if ((i < 1) || (i == descriptor.length())) {
			return false;
		}
		String v = descriptor.substring(i + 1);
		String n = descriptor.substring(0, i);
		return isValidName(n) && isValidVersion(v);
	}

	/**
	 * Give a path to a file, create an appropriate bundle descriptor.
	 * <p>
	 * This function is more complex than just looking at the file name. It
	 * generally checks the manifest file contained in the jar for the required
	 * information.
	 *
	 * @param f
	 *            the file
	 * @return a bundle descriptor
	 * @throws BundleValidationException
	 *             if the jar does not contain a valid manifest file
	 */
	public static BundleDescriptor fromJar(String filePath)
			throws BundleValidationException {
		return fromFile(new File(filePath));
	}

	/**
	 * Create a specific bundle descriptor from the given bundle file.
	 *
	 * @param f
	 *            a jar file
	 * @return a specific bundle descriptor if applicable
	 * @throws BundleValidationException
	 *             if the file's manifest contains invalid fields
	 */
	public static BundleDescriptor fromFile(File f)
			throws BundleValidationException {
		JarFile jar = null;
		BundleDescriptor ret = null;
		try {
			jar = new JarFile(f);
			ret = BundleDescriptor.fromManifest(jar.getManifest(), null);
			ret.setFile(f);
		} catch (IOException e) {
			throw new BundleValidationException(String.format(
					"IO error for file: %s", f.getName()), e);
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					LOG.error("The impossible has happened! A file "
							+ "descriptor could not be closed.");
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	/**
	 * This function retrieves a bundle descriptor from a given manifest object.
	 * <p>
	 * This is the the worker function from {@Code fromJar} without the
	 * <tt>IO</tt> handling and it is used by the {@link BUndleManager}.
	 * </p>
	 * <p>
	 * If the dependency manager is not null, the bundle descriptor is looked up
	 * in the database and if present, the.
	 * </p>
	 *
	 * @param the
	 *            manifest from a jar file
	 * @param dm
	 *            a dependency manager (may be null)
	 * @return the extracted BundleDescriptor
	 * @throws BundleValidationException
	 *             if the bundle version or name has an invalid format
	 */
	public static BundleDescriptor fromManifest(Manifest manifest,
			DependencyManager dm) throws BundleValidationException {
		Attributes map = manifest.getMainAttributes();
		String version = map.getValue("Bundle-Version");
		BundleDescriptor retVal = null;
		if (version == null) {
			throw new BundleValidationException(
					"unspecified version in the manifest");
		}
		String bname = map.getValue("Bundle-Name");
		if (bname == null) {
			throw new BundleValidationException(
					"unspecified bundle name in the manifest");
		}
		if (!NAMEFORMAT.matcher(bname).matches()) {
			throw new BundleValidationException(bname
					+ " is an invalid bundle name. It must obey the pattern: "
					+ NAMEFORMAT.pattern());
		}
		String type = map.getValue("Type");
		if (type != null) {
			type = type.toLowerCase();
			switch (type) {
			case "graph":
				retVal = new GraphBundleDescriptor(bname, version, null);
				break;
			case "parser":
				retVal = new ParserBundleDescriptor(bname, version, null);
				break;
			default:
				throw new BundleValidationException("Unknown 'Type' field: "
						+ type);
			}
		}
		String algType = map.getValue("Algorithm-Type");
		if (algType != null && algType.length() > 0) {
			String arityString = map.getValue("Arity");
			int arity = 0;
			if (arityString != null && arityString.length() > 0) {
				try {
					arity = Integer.parseInt(arityString);
				} catch (NumberFormatException e) {
					throw new BundleValidationException(
							"Could not parse arity field.", e);
				}
			}
			retVal = new AlgorithmBundleDescriptor(bname, version, null,
					algType, arity);
		}
		retVal = retVal != null ? retVal : new BundleDescriptor(bname, version);
		if (dm != null) {
			try {
				File f = dm.getFile(retVal);
				retVal.setFile(f);
			} catch (IOException e) {
				return retVal;
			}
		}
		return retVal;
	}

	@Override
	public String toString() {
		/* Mainly for debugging convenience. */
		return this.getDescriptor();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BundleDescriptor) {
			return ((BundleDescriptor) o).getDescriptor().equals(
					this.getDescriptor());
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return this.getDescriptor().hashCode();
	}

	/**
	 * Create a new specific bundle descriptor for this generic one.
	 * <p>
	 * If this object is already a specific one, this will be returned. <br>
	 * Note: The specific object may still be generic as it is not strictly
	 * necessary to define a type.
	 * </p>
	 *
	 * @return a new specific bundle descriptor (or this object if it is already
	 *         specific)
	 */
	public BundleDescriptor createSpecific(DependencyManager dm) {
		try {
			File bundleFile = file == null ? dm.getFile(this) : file;
			if (bundleFile == null) {
				throw new BundleValidationException(
						"Could not find a suitable file for this bundle in the "
								+ "database: " + this.getDescriptor());
			}
			switch (kind()) {
			case ALGORITHM:
			case GRAPH:
			case PARSER:
				// this is already specific
				return this;
			case GENERIC:
				return fromFile(bundleFile);
			default:
				throw new RuntimeException("Missing case statement for: "
						+ kind().toString());
			}
		} catch (BundleValidationException e) {
			throw new RuntimeException(String.format(
					"The bundle '%s' in the data base are corrupt!",
					getDescriptor()));
		} catch (IOException e) {
			// XXX Can something more meaningful be done here?
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Private class that overrides the <tt>kind()</tt>-method from the generic
	 * super class.
	 */
	private static class ParserBundleDescriptor extends BundleDescriptor {
		/**
		 *
		 */
		private static final long serialVersionUID = -4785277344520781298L;

		private ParserBundleDescriptor(String name, String version, File f)
				throws BundleValidationException {
			super(name, version, f);
		}

		@Override
		public Kind kind() {
			return Kind.PARSER;
		}
	}

	/**
	 * Private class that overrides the <tt>kind()</tt>-method from the generic
	 * super class.
	 */
	private static class GraphBundleDescriptor extends BundleDescriptor {
		/**
		 *
		 */
		private static final long serialVersionUID = 1756364405482664333L;

		private GraphBundleDescriptor(String name, String version, File f)
				throws BundleValidationException {
			super(name, version, f);
		}

		@Override
		public Kind kind() {
			return Kind.GRAPH;
		}
	}
}
