/**
 *
 */
package com.github.monet.controlserver;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.common.BundleValidationException;
import com.github.monet.common.BundleVersion;
import com.github.monet.common.VersionedPackage;

/**
 *
 */
public class ManifestParser implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -8485740918929839573L;
	private static Logger LOG = LogManager.getLogger(ManifestParser.class);
	/**
	 * The manifest file that this parser object uses.
	 */
	private Manifest manifest;

	/**
	 * Create a manifest parser for the given jar file.
	 *
	 * @param bundle
	 *            the bundle as a jar file
	 */
	public ManifestParser(JarFile bundle) {
		try {
			manifest = bundle.getManifest();
		} catch (IOException e) {
			throw new RuntimeException(
					"Could not retrieve the manifest form the jar file: "
							+ bundle.getName());
		}

	}

	/**
	 * @return the manifest
	 */
	public Manifest getManifest() {
		return manifest;
	}

	/**
	 * Parse the value of the given field as a collection of its fields together
	 * with their values.
	 *
	 * @param field
	 *            field key in the manifest file
	 * @return a list of elements which by themselves may contain a kind of
	 *         mapping
	 * @throws ParseException
	 */
	public Collection<Map<String, String>> parseList(String field)
			throws ParseException {
		Collection<Map<String, String>> fieldElements = new LinkedList<>();
		String content = manifest.getMainAttributes().getValue(field);

		if (content == null) {
			content = "";
			LOG.debug("Assuming empty value field for \"" + field + "\"");
		}
		Collection<String> elements = splitIntoElements(content);
		for (final String e : elements) {
			fieldElements.add(parseKeyValuePairs(e));
		}
		return fieldElements;
	}

	/**
	 * Split the value of the given field into the proper element fields.
	 * <p>
	 * For example: <tt>foobar;version="(2,5]",...</tt> will be split into
	 * <tt>foobar;version="(2,5]"</tt> and the rest of the list denoted by the
	 * tree dots. As you can see in this example, this cannot be simply split by
	 * using the <tt>split()</tt>-operation on Strings.
	 *
	 * @param content
	 *            the content of the manifest field to split
	 * @return a collection of Strings
	 */
	private Collection<String> splitIntoElements(String content)
			throws ParseException {
		Collection<String> elements = new LinkedList<>();
		boolean nestedString = false;
		int i;
		StringBuilder sb = new StringBuilder(content.length());
		for (i = 0; i < content.length(); i++) {
			final char c = content.charAt(i);
			switch (c) {
			case ',':
				if (!nestedString) {
					elements.add(sb.toString());
					sb = new StringBuilder(content.length() - sb.length());
				} else {
					sb.append(c);
				}
				break;
			case '"':
				nestedString = !nestedString;
			default:
				sb.append(c);
			}
		}
		if (nestedString) {
			throw new ParseException("Content ended being in a nested String.",
					i);
		}
		if (sb.length() > 0) {
			elements.add(sb.toString());
		}
		return elements;
	}

	/**
	 * Split the value of the given field element into the proper key-value
	 * pairs.
	 * <p>
	 * For example: <tt>foobar;version="(2,5]";something;weird="really",...</tt>
	 * will be split into the map:
	 *
	 * <pre>
	 * foobar =
	 * version = (2,5]
	 * something =
	 * weird = really
	 * </pre>
	 *
	 * @param fieldElement
	 *            the field element
	 * @return a map for the field element's value pairs
	 */
	private Map<String, String> parseKeyValuePairs(String fieldElement)
			throws ParseException {
		Map<String, String> m = new TreeMap<>();
		String key = "";
		String value = "";
		StringBuilder sb = new StringBuilder();
		boolean nestedString = false;
		int i;
		for (i = 0; i < fieldElement.length(); i++) {
			final char c = fieldElement.charAt(i);
			switch (c) {
			case '=':
				if (!nestedString) {
					key = sb.toString();
					sb = new StringBuilder();
				} else {
					sb.append(c);
				}
				break;
			case '"':
				if (nestedString) {
					value = sb.toString();
					m.put(key, value);
					key = "";
					value = "";
					sb = new StringBuilder();
					nestedString = false;
				} else {
					nestedString = true;
				}
				break;
			case ';':
				if (!nestedString) {
					if (sb.length() > 0) {
						if (key.length() > 0) {
							m.put(key, sb.toString());
						} else {
							m.put(sb.toString(), "");
						}
						key = "";
						value = "";
						sb = new StringBuilder();
					}
					break;
				}
			default:
				sb.append(c);
			}
		}
		if (nestedString) {
			throw new ParseException(
					"Field element ended being in a nested String", i);
		}
		if (sb.length() > 0) {
			if (key.length() > 0) {
				m.put(key, sb.toString());
			} else {
				m.put(sb.toString(), "");
			}
		}
		return m;
	}

	/**
	 * This function validates the fields of the bundle's manifest and returns
	 * those that are missing.
	 * <p>
	 * If there are fields missing or if there are fields that are not properly
	 * configured, then this function returns a list of field names together
	 * with a helpful error message.
	 *
	 * @return a map filled with error messages
	 */
	public Map<String, String> missingFields() {
		Map<String, String> ret = new TreeMap<>();

		// Arity (optional or it must be a positive integer)
		int arity = 0;
		try {
			String tmp = manifest.getMainAttributes().getValue("Arity");
			if (tmp != null && tmp.length() > 0) {
				arity = Integer.parseInt(tmp);
			}
		} catch (NumberFormatException e) {
			ret.put("Arity",
					"The \"Arity\" field in the manifest is not parseable "
							+ "as a number.");
		}
		if (arity < 0) {
			ret.put("Arity", "The \"Arity\" field in the manifest is "
					+ "negative! It is not possible to solve "
					+ "problems with a negeative amount of criteria!");
		}
		return ret;
	}

	public Set<VersionedPackage> retrieveExports() throws ParseException,
			BundleValidationException {
		return retrieveVersionedPackages("Export-Package");
	}

	public Set<VersionedPackage> retrieveImports() throws ParseException,
			BundleValidationException {
		return retrieveVersionedPackages("Import-Package");
	}

	private Set<VersionedPackage> retrieveVersionedPackages(String field)
			throws ParseException, BundleValidationException {
		Collection<Map<String, String>> line = parseList(field);
		Set<VersionedPackage> ret = new TreeSet<>();
		for (Map<String, String> item : line) {
			String packageName = null;
			BundleVersion packageVersion = null;
			for (Entry<String, String> e : item.entrySet()) {
				if (e.getValue() == null || e.getValue().equals("")) {
					packageName = e.getKey();
				} else if (e.getKey().equalsIgnoreCase("uses:")) {
					// XXX (saep) verify whether skipping this field works
					LOG.debug("Skipping uses field");
				} else if (e.getKey().equalsIgnoreCase("version")) {
					packageVersion = BundleVersion.fromString(e.getValue());
				}
			}
			if (packageName != null
					&& !VersionedPackage.packagesProvidedByMONET().contains(
							packageName)) {
				ret.add(new VersionedPackage(packageName,
						packageVersion == null ? BundleVersion.getAnyVersion()
								: packageVersion));
			}
		}
		return ret;
	}
}
