/**
 *
 */
package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.monet.common.BundleValidationException;

/**
 * A class that forms the base of all parameters that are supported.
 * <p>
 * It is a representation of the XML schema definition which describes the
 * various parameter forms and possibilities. In this abstract class, the fields
 * used by all parameters are represented as well as some functions which are
 * extremely useful to work on these objects.
 * </p>
 * <p>
 * <b>Note</b>: Empty strings are treated the same was as a null pointer.
 */
public abstract class Parameter implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 6759209964811929939L;

	/**
	 * This enumeration lists all supported types of parameters.
	 */
	public enum Type {
		SPACELESSSTRING, STRING, INTEGER, BOUNDEDINTEGER, DECIMAL, BOUNDEDDECIMAL, BOOLEAN, CHOICE
	}

	/**
	 * Just a non empty string that captures the contents of the string without
	 * leading and trailing whitespace.
	 * <p>
	 * <tt>"\\s*([a-zA-Z0-9_\\.-]+)\\s*"</tt>
	 * </p>
	 */
	static Pattern NONEMPTYSTRING = Pattern
			.compile("\\s*([a-zA-Z0-9_\\.-]+)\\s*");

	/**
	 * The value that this object will store. It could be anything! However, it
	 * must be sent over the network and is therefore transformed into a string.
	 */
	private String value;

	/**
	 * The name of the parameter which will also be the key in the job's
	 * argument map.
	 */
	private final String name;

	/**
	 * The description for this parameter. It will be the documentation in the
	 * GUI for the parameters.
	 */
	private final String description;

	/**
	 * The default value.
	 */
	private final String defaultValue;

	/**
	 * Instantiate a AlgorithmParameter object with the given name and
	 * description.
	 * <p>
	 * This constructor must be used by all inheriting classes to properly set
	 * the name and description field.
	 *
	 * @param name
	 *            parameter name
	 * @param description
	 *            description for the parameter
	 * @param defaultValue
	 *            the default value
	 * @param type
	 *            the type this objects value should be compatible with
	 */
	public Parameter(String name, String description, String defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = null;
	}

	/**
	 * Validate the value set on this object.
	 * <p>
	 * Since the parameter map is verified against an <tt>X</tt>ML <tt>S</tt>
	 * chema <tt>D</tt>efinition, only the value field must be checked.
	 *
	 * @throws BundleValidationException
	 *             if the value is not properly set
	 */
	public abstract void verify() throws BundleValidationException;

	/**
	 * Set the value of this object to the given parameter.
	 * <p>
	 * This function sets the value to the default value if the input is null or
	 * the empty string and otherwise to the given value. It also strips leading
	 * and trailing whitespace.
	 *
	 * @param value
	 *            the value to set or null to use the default
	 * @throws IllegalArgumentException
	 *             if the input value does not conform to the allowed pattern
	 */
	public void setValue(final String value) {
		String strippedValue = null;
		if (value != null) {
			Matcher m = NONEMPTYSTRING.matcher(value);
			if (m.matches()) {
				strippedValue = m.group(1);
			} else {
				throw new IllegalArgumentException(
						"The value does not match the allowed pattern: "
								+ NONEMPTYSTRING.toString());
			}
		}

		if (strippedValue == null || strippedValue.length() == 0) {
			this.value = defaultValue();
		} else {
			this.value = strippedValue;
		}
	}

	/**
	 * This function blows up in your face if you do not have defined a default
	 * value or if you haven't set a proper value.
	 *
	 * @return the value of this parameter
	 */
	public final String getValue() {
		if (!isAssigned() && defaultValue() == null) {
			throw new RuntimeException("The value was never initialized and "
					+ "there is no default value defined.");
		}
		return !isAssigned() || value.length() == 0 ? defaultValue() : value;
	}

	/**
	 * This function should return null if no default value is present.
	 *
	 * @return the default value for the parameter
	 */
	public final String defaultValue() {
		return defaultValue;
	}

	/**
	 * This function is mainly useful to ensure safe casting in
	 * switch-case-statements.
	 *
	 * @return the type of the parameter
	 */
	public abstract Type getType();

	/**
	 * @return true if this parameter has been assigned a value
	 */
	public boolean isAssigned() {
		return value != null && value.length() > 0;
	}

	/**
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	public final String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return String.format("Type: %s Name: %s Value: %s DefaultValue: %s",
				getType().toString(), name, value, defaultValue);
	}

	/**
	 * This class represents the Parameter-API to a value of a string without
	 * leading and trailing whitespace.
	 */
	public static class StringParameter extends Parameter {

		/**
		 *
		 */
		private static final long serialVersionUID = 9223276343457630537L;

		/**
		 * Construct an AlgorithmParameter object for a string valued parameter.
		 *
		 * @param name
		 *            the name
		 * @param description
		 *            the description
		 * @param defaultValue
		 *            the default value
		 */
		public StringParameter(String name, String description,
				String defaultValue) {
			super(name, description, defaultValue);
		}

		@Override
		public void verify() throws BundleValidationException {
			Matcher m = NONEMPTYSTRING.matcher(getValue());
			if (!m.matches()) {
				throw new BundleValidationException(String.format(
						"%s does not match the pattern %s", getValue(),
						NONEMPTYSTRING.toString()));
			}
		}

		@Override
		public Type getType() {
			return Type.STRING;
		}

	}

	/**
	 * Simple <tt>true</tt> or <tt>false</tt> value.
	 */
	public static class BooleanParameter extends Parameter {

		/**
		 *
		 */
		private static final long serialVersionUID = -1864403280809021743L;

		/**
		 * @param name
		 *            the name
		 * @param description
		 *            the description
		 * @param defaultValue
		 *            the default value
		 */
		public BooleanParameter(String name, String description,
				String defaultValue) {
			super(name, description, defaultValue);
		}

		@Override
		public void verify() throws BundleValidationException {
			try {
				Boolean.parseBoolean(getValue());
			} catch (Exception e) {
				throw new BundleValidationException(e.getMessage(), e);
			}

		}

		@Override
		public Type getType() {
			return Type.BOOLEAN;
		}

	}

	/**
	 * Abstract parameter class that provides the min and max field.
	 *
	 * @param T
	 *            type of the parameter's value
	 */
	public abstract static class BoundedParameter<T extends Comparable<T>>
			extends Parameter {

		/**
		 *
		 */
		private static final long serialVersionUID = 6835099343428030919L;
		private final String min;
		private final String max;
		private boolean minIsInclusive;
		private boolean maxIsInclusive;

		public BoundedParameter(final String name, final String description,
				final String defaultValue, final String min,
				final String minIsInclusive, final String max,
				final String maxIsInclusive) {
			super(name, description, defaultValue);
			this.min = min;
			this.max = max;
			this.maxIsInclusive = maxIsInclusive.length() == 0
					|| Boolean.parseBoolean(maxIsInclusive);
			this.minIsInclusive = minIsInclusive.length() == 0
					|| Boolean.parseBoolean(minIsInclusive);
		}

		/**
		 * @return the minimum of possible values for this parameter
		 */
		public String getMin() {
			return min;
		}

		/**
		 * @return the maximum of possible value for this parameter
		 */
		public String getMax() {
			return max;
		}

		/**
		 * Validate that the value is in the proper range.
		 *
		 * @return true if it is
		 */
		protected boolean checkRange() {
			T maxVal = parse(getMax());
			T minVal = parse(getMin());
			T val = parse(getValue());
			boolean acc = true;
			int cmp = val.compareTo(minVal);
			acc &= cmp > 0 || minIsInclusive && cmp == 0;
			cmp = val.compareTo(maxVal);
			acc &= cmp < 0 || maxIsInclusive && cmp == 0;
			return acc;
		}

		/**
		 * Function that parses the given string to a value of type <tt>T</tt>.
		 *
		 * @param numberString
		 *            the string to parse
		 * @return the number of type T
		 */
		protected abstract T parse(String numberString);

	}

	/**
	 * Simple floating point value with double precision.
	 */
	public static class DecimalParameter extends Parameter {

		/**
		 *
		 */
		private static final long serialVersionUID = -8771614223328978777L;

		/**
		 * Create floating point parameter for the given arguments.
		 *
		 * @param name
		 *            name of the parameter
		 * @param description
		 *            a descriptive text
		 * @param defaultValue
		 *            the default value (null is allowed to indicate no value)
		 */
		public DecimalParameter(String name, String description,
				String defaultValue) {
			super(name, description, defaultValue);
		}

		@Override
		public void verify() throws BundleValidationException {
			try {
				Double.parseDouble(getValue());
			} catch (Exception e) {
				throw new BundleValidationException(e.getMessage(), e);
			}
		}

		@Override
		public Type getType() {
			return Type.DECIMAL;
		}

	}

	/**
	 * A floating point value with upper and lower bounds.
	 *
	 * @see DecimalParameter
	 */
	public static class BoundedDecimalParameter extends
			BoundedParameter<Double> {

		/**
		 *
		 */
		private static final long serialVersionUID = -4850122269126074503L;

		public BoundedDecimalParameter(String name, String description,
				String defaultValue, String min, String minIsInclusive,
				String max, String maxIsInclusive) {
			super(name, description, defaultValue, min, minIsInclusive, max,
					maxIsInclusive);
		}

		@Override
		public void verify() throws BundleValidationException {
			try {
				if (!checkRange()) {
					throw new Exception("Value is not within allowed range.");
				}
			} catch (Exception e) {
				throw new BundleValidationException(e.getMessage(), e);
			}
		}

		@Override
		public Type getType() {
			return Type.BOUNDEDDECIMAL;
		}

		@Override
		protected Double parse(String numberString) {
			return Double.parseDouble(numberString);
		}
	}

	/**
	 * A simple integer parameter with 64 bit bounds.
	 */
	public static class IntegerParameter extends Parameter {

		/**
		 *
		 */
		private static final long serialVersionUID = 3335879136100294184L;

		/**
		 * Construct an integer parameter.
		 *
		 * @param name
		 *            name of the parameter
		 * @param description
		 *            a descriptive text
		 * @param defaultValue
		 *            a default value (null indicates that there is no default)
		 */
		public IntegerParameter(String name, String description,
				String defaultValue) {
			super(name, description, defaultValue);
		}

		@Override
		public void verify() throws BundleValidationException {
			try {
				Long.parseLong(getValue());
			} catch (Exception e) {
				throw new BundleValidationException(e.getMessage(), e);
			}
		}

		@Override
		public Type getType() {
			return Type.INTEGER;
		}

	}

	/**
	 * This class represents integer values with lower and upper bounds.
	 * <p>
	 * As it is a nuisance to define a lot of operators for optional lower and
	 * upper bounds, just use <tt>Long.MAX_VALUE </tt>or
	 * <tt>Long.MIN_VALUE </tt>respectively with inclusive bounds.
	 * </p>
	 */
	public static class BoundedIntegerParameter extends BoundedParameter<Long> {

		/**
		 *
		 */
		private static final long serialVersionUID = -6413234380261235987L;

		/**
		 * Create a bounded integer parameter.
		 *
		 * @param name
		 *            the name of the parameter
		 * @param description
		 *            a description
		 * @param defaultValue
		 *            the default value (null indicates that there is no default
		 *            value)
		 * @param min
		 *            lower bound
		 * @param minIsInclusive
		 *            indicator whether the lower bound is inclusive
		 * @param max
		 *            upper bound
		 * @param maxIsInclusive
		 *            indicator whether the upper bound is inclusive
		 */
		public BoundedIntegerParameter(String name, String description,
				String defaultValue, String min, String minIsInclusive,
				String max, String maxIsInclusive) {
			super(name, description, defaultValue, min, minIsInclusive, max,
					maxIsInclusive);
		}

		@Override
		public void verify() throws BundleValidationException {
			try {
				if (!checkRange()) {
					throw new Exception("Value is not within allowed range.");
				}
			} catch (Exception e) {
				throw new BundleValidationException(e.getMessage(), e);
			}
		}

		@Override
		public Type getType() {
			return Type.BOUNDEDINTEGER;
		}

		@Override
		protected Long parse(String numberString) {
			return Long.parseLong(numberString);
		}

	}

	/**
	 * This class provides convenient functions to access the possible choice
	 * parameters.
	 */
	public static class ChoiceParameter extends Parameter {
		/**
		 *
		 */
		private static final long serialVersionUID = 3079067655312294237L;
		/**
		 * If the value is a choice, then this field is not null and lists the
		 * choices.
		 */
		private final List<String> choices;

		/**
		 * Create a choice parameter.
		 *
		 * @param name
		 *            the name of the parameter
		 * @param description
		 *            the description
		 * @param defaultValue
		 *            the default value (null or a value that does not exist
		 *            indicate that there is no default value)
		 * @param choices
		 *            a list of available choices
		 */
		public ChoiceParameter(String name, String description,
				String defaultValue, final List<String> choices) {
			super(name, description,
					choices.contains(defaultValue) ? defaultValue : (choices
							.size() == 1 ? choices.get(0) : "-"));
			this.choices = choices;
		}

		/**
		 * @return a list of possible choices
		 */
		public final List<String> getChoices() {
			return choices;
		}

		@Override
		public void verify() throws BundleValidationException {
			Matcher m = NONEMPTYSTRING.matcher(getValue());
			if (!m.matches()) {
				throw new BundleValidationException(String.format(
						"%s does not match the pattern %s", getValue(),
						NONEMPTYSTRING.toString()));
			}
			setValue(m.group(1));
			if (!choices.contains(getValue())) {
				throw new BundleValidationException(getValue()
						+ " is not in the list of possible choices");
			}
		}

		@Override
		public Type getType() {
			return Type.CHOICE;
		}

	}
}
