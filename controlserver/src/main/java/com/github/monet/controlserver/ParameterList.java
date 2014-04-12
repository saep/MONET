/**
 *
 */
package com.github.monet.controlserver;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.github.monet.common.BundleValidationException;
import com.github.monet.controlserver.Parameter.ChoiceParameter;

/**
 * This function uses the ParameterIterator to show all parameter options that
 * can be set for the given set of chosen parameters.
 * <p>
 * It is important to only instantiate one object of this class as it saves the
 * state of chosen parameters. To update chosen parameters, you must call the
 * <tt>setvalue(String)</tt>-method on the parameter objects returned by
 * <tt>getParameterList()</tt>. If you think that all parameters have been set,
 * call <tt>validateparameters()</tt>. This throws a ParameterException that
 * contains useful information to show meaningful error messages in the user
 * interface. When it did not throw an exception, you can get the set parameters
 * as a simple map from string to string.
 * <br><b>Note</b>: You should call the <tt>close</tt> function after you're
 * done to free resources.
 * </p>
 *
 * @see ParameterIterator
 */
public abstract class ParameterList implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 7594975852571338895L;

	/**
	 * The name field of the algorithm/graph.
	 */
	private String name;

	/**
	 * The description field of the algorithm/graph.
	 */
	private String description;

	/**
	 * A cache for the currently set values.
	 */
	private Map<String, String> cache;

	/**
	 * The parameter iterator used to traverse the XML-DOM-tree.
	 */
	private ParameterIterator parameterIterator;

	/**
	 * The last returned parameter map.
	 * <p>
	 * This is used to update the parameter cache upon calling
	 * <tt>validateParameters</tt>.
	 */
	private List<Parameter> last;

	/**
	 * Desperate try to keep the stream open...
	 */
	private File xml;

	private boolean validated;

	/**
	 * This constructor sets the private members according to the values in the
	 * parameter iterator.
	 */
	protected ParameterList(ParameterIterator it) {
		this.xml = it.getTemporaryXMLFile();
		this.name = it.getName();
		this.description = it.getDescription();
		this.cache = new TreeMap<>();
		this.last = null;
	}

	/**
	 * Provides a list of parameters to set.
	 *
	 * @see {@link AlgorithmParameterIterator#next()}
	 * @return the parameter list
	 */
	public List<Parameter> getParameterList() {
		try {
			parameterIterator = createIteratorInstance(xml);
		} catch (BundleValidationException e) {
			e.printStackTrace();
			/*
			 * As long as the file is not deleted whilst executing this
			 * function, there should not be any exception thrown at this point.
			 */
			return null;
		}
		updateCache();
		List<Parameter> parameters = new LinkedList<>();
		while (parameterIterator.hasNext()) {
			List<Parameter> params = parameterIterator.next();
			for (Parameter p : params) {
				String cachedParam = cache.get(p.getName());
				if (cachedParam != null) {
					p.setValue(cachedParam);
				} else if (p instanceof ChoiceParameter) {
					p.setValue(null);
				}
				parameters.add(p);
			}
			try {
				parameterIterator.validateParameters(true);
			} catch (ParameterException e) {
				e.printStackTrace();
				return null;
			}
		}
		last = Collections.unmodifiableList(parameters);
		return last;
	}

	/**
	 * Update the cached values with those set in the parameter list
	 * <tt>last</tt>.
	 */
	private void updateCache() {
		if (last != null) {
			for (Parameter p : last) {
				if (p.isAssigned()) {
					cache.put(p.getName(), p.getValue());
				}
			}
		}
	}

	protected abstract ParameterIterator createIteratorInstance(File xml)
			throws BundleValidationException;

	/**
	 * Validates the parameters and updates the map.
	 *
	 * @throws ParameterException
	 */
	public void validateParameters() throws ParameterException {
		cache.clear();
		updateCache();
		ParameterIterator.validate(last);
		if (parameterIterator.hasNext()) {
			throw new ParameterException(parameterIterator.next());
		}
		validated = true;
	}

	/**
	 * Provides the parameter map.
	 *
	 * @return the parameter map
	 * @throws RuntimeException
	 *             if the parameters were not validated via validateParameters
	 */
	public Map<String, String> getParameterMap() {
		if (validated == false) {
			throw new RuntimeException();
		}
		return cache;
	}

	/**
	 * Close the resources allocated by this object.
	 */
	public void close() {
		if (xml != null) {
			xml.delete();
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}
