/**
 *
 */
package com.github.monet.common;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is actually a bijective projection of version strings to a java
 * type with convenient version handling functions.
 */
public abstract class BundleVersion implements Cloneable,Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 5163626094132141693L;

	private static Logger LOG = LogManager.getLogger(BundleVersion.class);

	private static Pattern versionStructure = Pattern
			.compile("([\\[\\(])?([^\\]\\)]+)([\\]\\)])?");

	private static Pattern leadingZero = Pattern.compile("0*(\\d+)");

	private static BundleVersion instanceAnyVersion = new AnyBundleVersion();
	private static BundleVersion instanceInvalidVersion = new None();

	/**
	 * \\d+(\\.\\d+)*([-\\.]\\w+)?
	 */
	public static final Pattern VERSIONFORMAT = Pattern
			.compile("\\d+(\\.\\d+)*([-\\.]\\w+)?");

	/**
	 * The bundle version object is intended to be created from a string
	 */
	private BundleVersion() {
	}

	/**
	 * Enumeration representing different kinds of version constraints.
	 */
	public enum Kind {
		/**
		 * no version constraint
		 */
		ANY,
		/**
		 * specific version
		 */
		SINGLE,
		/**
		 * a range of versions
		 */
		RANGE,
		/**
		 * lower bound on the version
		 */
		LOWERBOUND,
		/**
		 * upper bound on the version
		 */
		UPPERBOUND,
		/**
		 * invalid version
		 */
		NONE
	};

	/**
	 * Convenience method that can be used to create version objects that have
	 * (almost) no information.
	 *
	 * @return a bundle version object without a version
	 */
	public static BundleVersion getAnyVersion() {
		return instanceAnyVersion;
	}

	/**
	 * Create a new BundleVersion object that represents an invalid version.
	 *
	 * @return a new invalid version object
	 */
	public static BundleVersion getInvalidVersion() {
		return instanceInvalidVersion;
	}

	/**
	 * Remove leading 0s and whitespace from bundle version strings and
	 * conveniently put them in a character array.
	 *
	 * @return the canonicalized version string as an char array
	 */
	public static String canonicalizeVersionString(String version) {
		StringBuilder canonical = new StringBuilder(version.length());
		int split = version.indexOf("-");
		String suffix = split < 0 ? "" : version.substring(split);
		String prefix = split < 0 ? version : version.substring(0, split);
		String[] vers = prefix.split("\\.");
		for (int i = 0; i < vers.length; i++) {
			Matcher m = leadingZero.matcher(vers[i]);
			if (m.matches()) {
				canonical.append(m.group(1));
			} else {
				canonical.append(vers[i]);
			}
			if (i + 1 < vers.length) {
				canonical.append(".");
			}
		}
		return canonical.append(suffix).toString();
	}

	/**
	 * Parse the given input string an convert it into a <tt>BundleVersion</tt>
	 * object.
	 *
	 * @param inp
	 *            input string
	 * @return a bundle version object
	 * @throws BundleValidationException
	 */
	public static BundleVersion fromString(String inp)
			throws BundleValidationException {
		if (inp == null) {
			throw new BundleValidationException(
					"Input version string was null.");
		}
		if ((inp.length() == 0) || inp.equals("*")) {
			return getAnyVersion();
		}
		Matcher m = versionStructure.matcher(inp);
		if (!m.matches()) {
			throw new BundleValidationException(
					"Version strcuture does not match the required pattern.");
		}

		final String lowerBoundCharacter = m.group(1);
		final String upperBoundCharacter = m.group(3);
		if (lowerBoundCharacter != null) {
			if (upperBoundCharacter != null) {
				String[] bounds = m.group(2).split(",");
				if (bounds.length != 2) {
					throw new BundleValidationException(
							"Version string for range object did not contain"
									+ " two valid version strings");
				}
				String lower = bounds[0];
				String upper = bounds[1];
				Matcher ml = VERSIONFORMAT.matcher(lower);
				Matcher mr = VERSIONFORMAT.matcher(upper);
				if (!ml.matches() || !mr.matches()) {
					throw new BundleValidationException(
							"Version string for range object did not contain"
									+ " two valid version strings");
				}
				LowerBoundBundleVersion l = new LowerBoundBundleVersion(lower,
						lowerBoundCharacter.equals("["));
				UpperBoundBundleVersion u = new UpperBoundBundleVersion(upper,
						upperBoundCharacter.equals("]"));
				int comp = l.compareTo(u);
				if (comp == 0) {
					if (l.lowerBoundIsInclusive() && u.upperBoundIsInclusive()) {
						return new SingleBundleVersion(lower);
					}
					throw new BundleValidationException(
							"Upper and lower bound are the same but not "
									+ "inclusive.");
				} else if (comp < 0) {
					return new RangeBundleVersion(l, u);
				} else {
					return getInvalidVersion();
				}
			} else {
				Matcher mv = VERSIONFORMAT.matcher(m.group(2));
				if (mv.matches()) {
					return new LowerBoundBundleVersion(m.group(2),
							lowerBoundCharacter.equals("["));
				}
				throw new BundleValidationException(
						"Version does not match pattern: "
								+ VERSIONFORMAT.toString());
			}
		} else if (upperBoundCharacter != null) {
			Matcher mv = VERSIONFORMAT.matcher(m.group(2));
			if (mv.matches()) {
				return new UpperBoundBundleVersion(m.group(2),
						upperBoundCharacter.equals("]"));
			}
			throw new BundleValidationException(
					"Version does not match pattern: "
							+ VERSIONFORMAT.toString());
		} else {
			return new SingleBundleVersion(m.group(2));
		}
	}

	/**
	 * Merge the two versions together so that both restrictions are
	 * incorporated into the new one.
	 * <p>
	 * <b>Properties</b>:
	 * <ul>
	 * <li><b>identity</b>: <tt>v.merge(v) = v</tt></li>
	 * <li><b>neutral element</b>:
	 * <tt>v.merge(getAnyVersion()) = getAnyVersion().merge(v)</tt></li>
	 * <li><b>symmetry:</b>: <tt>v.merge(w) = w.merge(v)</tt></li>
	 * <li>&forall; v:
	 * <tt>getInvalidVersion().merge(v) = v.merge(getInvalidVersion()) = getInvalidVersion()</tt>
	 * </li>
	 * </ul>
	 *
	 * @param v
	 *            the version to merge with
	 * @return a new BundleVersion object or null if it cannot be merged
	 */
	public abstract BundleVersion merge(BundleVersion v);

	/**
	 * Return true if the raw version string is compatible with this object.
	 *
	 * @param rawVersion
	 *            the version to check upon
	 * @return indicator whether the versions are compatible
	 */
	public final boolean isCompatibleWith(String rawVersion) {
		try {
			return isCompatibleWith(fromString(rawVersion));
		} catch (BundleValidationException e) {
			return false;
		}
	}

	/**
	 * Return true if the raw version string is compatible with this object.
	 *
	 * @param bv
	 *            the version to check upon
	 * @return indicator whether the versions are compatible
	 */
	public final boolean isCompatibleWith(BundleVersion bv) {
		return merge(bv) != getInvalidVersion();
	}

	/**
	 * @return true if the lower bound is inclusive
	 */
	public abstract boolean lowerBoundIsInclusive();

	/**
	 * @return true if the upper bound is inclusive
	 */
	public abstract boolean upperBoundIsInclusive();

	/**
	 * @return the kind of this version object
	 * @see Kind
	 */
	public abstract Kind kind();

	/**
	 * The show operation is designed to be a bijective function of the version
	 * string as described in the manifest file and the the version string
	 * returned by this function.
	 *
	 * @return a string representation of the version
	 */
	public abstract String show();

	@Override
	public BundleVersion clone() {
		try {
			return fromString(show());
		} catch (BundleValidationException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return show();
	}

	/**
	 * Convenience method to output a uniform error message for unsupported
	 * bundle versions.
	 *
	 * @param v
	 *            the unsupported bundle version
	 * @return a runtime exception with a meaningful message
	 */
	static RuntimeException logUnsupportedVersion(BundleVersion v) {
		RuntimeException e = new RuntimeException(
				"The kind of bundle version is not supported: " + v.kind());
		LOG.error(e);
		return e;
	}

	/**
	 *
	 */
	private static class None extends BundleVersion {

		/**
		 *
		 */
		private static final long serialVersionUID = 3594121251274796156L;

		@Override
		public BundleVersion merge(BundleVersion v) {
			return this;
		}

		@Override
		public boolean lowerBoundIsInclusive() {
			return false;
		}

		@Override
		public boolean upperBoundIsInclusive() {
			return false;
		}

		@Override
		public Kind kind() {
			return Kind.NONE;
		}

		@Override
		public String show() {
			return null;
		}

		@Override
		public BundleVersion clone() {
			return getInvalidVersion();
		}

	}

	/**
	 * An object from this class represents a version without any restrictions.
	 */
	private static class AnyBundleVersion extends BundleVersion {

		/**
		 *
		 */
		private static final long serialVersionUID = -2286054477551770661L;

		@Override
		public BundleVersion merge(BundleVersion v) {
			return v.clone();
		}

		@Override
		public boolean lowerBoundIsInclusive() {
			return false;
		}

		@Override
		public boolean upperBoundIsInclusive() {
			return false;
		}

		@Override
		public Kind kind() {
			return Kind.ANY;
		}

		@Override
		public String show() {
			/*
			 * This is not very informative, but it is compatible with the
			 * bijective assumption of version strings.
			 */
			return "";
		}

		@Override
		public BundleVersion clone() {
			return getAnyVersion();
		}
	}

	/**
	 * This class represents an object that only allows one specific version to
	 * be compatible.
	 */
	private static class SingleBundleVersion extends BundleVersion implements
			Comparable<SingleBundleVersion> {

		/**
		 *
		 */
		private static final long serialVersionUID = 4216801653542080710L;
		/**
		 * the version string
		 */
		private String version;

		/**
		 * Create a <tt>SingleBundleVersion</tt>.
		 * <p>
		 * This constructor also validates the version string.
		 *
		 * @param version
		 *            the raw version string
		 * @throws BundleValidationException
		 *             if the version string is invalid
		 */
		public SingleBundleVersion(String version)
				throws BundleValidationException {
			Matcher m = VERSIONFORMAT.matcher(version);
			if (m.matches()) {
				this.version = canonicalizeVersionString(version);
			} else {
				throw new BundleValidationException("Invalid version string.");
			}

		}

		@Override
		public BundleVersion merge(BundleVersion v) {
			switch (v.kind()) {
			case LOWERBOUND:
			case UPPERBOUND:
			case RANGE:
				return v.merge(this);
			case SINGLE:
				int comp = compareTo((SingleBundleVersion) v);
				return comp == 0 ? clone() : getInvalidVersion();
			case ANY:
				return clone();
			case NONE:
				return getInvalidVersion();
			}
			throw logUnsupportedVersion(v);
		}

		@Override
		public int compareTo(SingleBundleVersion o) {
			char[] vthis = getVersionWithoutBounds().toCharArray();
			char[] vother = o.getVersionWithoutBounds().toCharArray();
			int min = vthis.length < vother.length ? vthis.length
					: vother.length;

			for (int i = 0; i < min; i++) {
				if (vthis[i] == vother[i]) {
					continue;
				}
				if (vthis[i] == '.') {
					return 1;
				} else if (vother[i] == '.') {
					return -1;
				}
				return Character.compare(vthis[i], vother[i]);

			}
			return Integer.signum(vthis.length - vother.length);
		}

		@Override
		public boolean lowerBoundIsInclusive() {
			return true;
		}

		@Override
		public boolean upperBoundIsInclusive() {
			return true;
		}

		@Override
		public Kind kind() {
			return Kind.SINGLE;
		}

		@Override
		public String show() {
			return version;
		}

		/**
		 * @return the version string without the possibly present bound
		 *         characters
		 */
		public String getVersionWithoutBounds() {
			return version;
		}
	}

	/**
	 * Super class for the lower and upper bound version classes that avoids
	 * some code duplication.
	 */
	private static abstract class SingleBoundBundleVersion extends
			SingleBundleVersion {
		/**
		 *
		 */
		private static final long serialVersionUID = -6760961988400892925L;
		/**
		 * Indicator whether the bound is inclusive.
		 */
		protected final boolean inclusive;

		/**
		 * Construct a version with a single bound by supplying the version
		 * string together with an indicator whether the boundary is inclusive.
		 *
		 * @param version
		 *            the version string
		 * @param inclusive
		 *            indicator whether the boundary is inclusive
		 * @throws BundleValidationException
		 *             if the version string does not conform to the version
		 *             scheme
		 */
		public SingleBoundBundleVersion(String version, boolean inclusive)
				throws BundleValidationException {
			super(version);
			this.inclusive = inclusive;
		}
	}

	/**
	 * This class represents versions that are only bound by a minimum version.
	 */
	private static class LowerBoundBundleVersion extends
			SingleBoundBundleVersion {

		/**
		 *
		 */
		private static final long serialVersionUID = -409487587222622295L;

		/**
		 * Construct a version with a single bound by supplying the version
		 * string together with an indicator whether the boundary is inclusive.
		 *
		 * @param version
		 *            the version string
		 * @param inclusive
		 *            indicator whether the boundary is inclusive
		 * @throws BundleValidationException
		 *             if the version string does not conform to the version
		 *             scheme
		 */
		public LowerBoundBundleVersion(String version, boolean inclusive)
				throws BundleValidationException {
			super(version, inclusive);
		}

		@Override
		public BundleVersion merge(BundleVersion v) {
			int comp = 0;
			switch (v.kind()) {

			case LOWERBOUND:
				SingleBundleVersion lb = (SingleBundleVersion) v;
				comp = compareTo(lb);
				if ((comp < 0) || ((comp == 0) && !lb.lowerBoundIsInclusive())) {
					return lb.clone();
				}
			case ANY:
				return clone();

			case NONE:
				return getInvalidVersion();

			case RANGE:
				return v.merge(this);

			case SINGLE:
				comp = compareTo((SingleBundleVersion) v);
				if ((comp < 0) || ((comp == 0) && lowerBoundIsInclusive())) {
					return v.clone();
				}
				return getInvalidVersion();

			case UPPERBOUND:
				try {
					return fromString(show() + "," + v.show());
				} catch (BundleValidationException e) {
					return getInvalidVersion();
				}

			default:
				throw logUnsupportedVersion(v);
			}
		}

		@Override
		public boolean lowerBoundIsInclusive() {
			return inclusive;
		}

		@Override
		public boolean upperBoundIsInclusive() {
			return false;
		}

		@Override
		public Kind kind() {
			return Kind.LOWERBOUND;
		}

		@Override
		public String show() {
			return (lowerBoundIsInclusive() ? "[" : "(")
					+ getVersionWithoutBounds();
		}
	}

	/**
	 * This class represents versions that are only restricted by an upper
	 * version limit.
	 */
	private static class UpperBoundBundleVersion extends
			SingleBoundBundleVersion {

		/**
		 *
		 */
		private static final long serialVersionUID = -7023360329665486836L;

		/**
		 * Construct a version with a single bound by supplying the version
		 * string together with an indicator whether the boundary is inclusive.
		 *
		 * @param version
		 *            the version string
		 * @param inclusive
		 *            indicator whether the boundary is inclusive
		 * @throws BundleValidationException
		 *             if the version string does not conform to the version
		 *             scheme
		 */
		public UpperBoundBundleVersion(String version, boolean inclusive)
				throws BundleValidationException {
			super(version, inclusive);
		}

		@Override
		public BundleVersion merge(BundleVersion v) {
			int comp = 0;
			switch (v.kind()) {

			case UPPERBOUND:
				SingleBundleVersion ub = (SingleBundleVersion) v;
				comp = compareTo(ub);
				if ((comp > 0) || ((comp == 0) && !ub.upperBoundIsInclusive())) {
					return ub.clone();
				}
			case ANY:
				return clone();

			case NONE:
				return getInvalidVersion();

			case RANGE:
				return v.merge(this);

			case SINGLE:
				comp = compareTo((SingleBundleVersion) v);
				if ((comp < 0) || ((comp == 0) && lowerBoundIsInclusive())) {
					return v.clone();
				}
				return getInvalidVersion();

			case LOWERBOUND:
				return v.merge(this);

			default:
				throw logUnsupportedVersion(v);
			}
		}

		@Override
		public boolean lowerBoundIsInclusive() {
			return false;
		}

		@Override
		public boolean upperBoundIsInclusive() {
			return inclusive;
		}

		@Override
		public Kind kind() {
			return Kind.UPPERBOUND;
		}

		@Override
		public String show() {
			return getVersionWithoutBounds()
					+ (upperBoundIsInclusive() ? "]" : ")");
		}
	}

	/**
	 * This class represents objects that are bound by two version restrictions
	 * where one defines a minimum required version and the other defines a
	 * maximum allowed version.
	 */
	private static class RangeBundleVersion extends BundleVersion {
		/**
		 *
		 */
		private static final long serialVersionUID = -7931143246837750126L;
		/**
		 * lower bound version string
		 */
		final private LowerBoundBundleVersion lower;
		/**
		 * upper bound version string
		 */
		final private UpperBoundBundleVersion upper;

		/**
		 * Create a bundle version object representing a range.
		 *
		 * @param lower
		 *            the lower bound version string
		 * @param upper
		 *            the upper bound version string
		 * @param lowerInclusive
		 *            indicator whether the lower bound is inclusive
		 * @param upperInclusive
		 *            indicator whether the upper bound is inclusive
		 */
		public RangeBundleVersion(LowerBoundBundleVersion lower,
				UpperBoundBundleVersion upper) {
			this.lower = lower;
			this.upper = upper;
		}

		@Override
		public BundleVersion merge(BundleVersion v) {
			try {
				switch (v.kind()) {
				case LOWERBOUND:
					return fromString(lower.merge(v).show() + ","
							+ upper.show());

				case ANY:
					return clone();

				case NONE:
					return getInvalidVersion();

				case RANGE:
					return v.merge(lower).merge(upper);

				case SINGLE:
					return lower.merge(upper.merge(v));

				case UPPERBOUND:
					return fromString(lower.show() + ","
							+ upper.merge(v).show());

				default:
					throw logUnsupportedVersion(v);
				}

			} catch (BundleValidationException e) {
				return getInvalidVersion();
			}
		}

		@Override
		public boolean lowerBoundIsInclusive() {
			return lower.lowerBoundIsInclusive();
		}

		@Override
		public boolean upperBoundIsInclusive() {
			return upper.upperBoundIsInclusive();
		}

		@Override
		public Kind kind() {
			return Kind.RANGE;
		}

		@Override
		public String show() {
			return lower.show() + "," + upper.show();
		}

	}

}
