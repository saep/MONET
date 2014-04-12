/**
 *
 */
package com.github.monet.controlserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.bind.ValidationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.BundleValidationException;
import com.github.monet.common.DependencyManager;
import com.github.monet.common.FileUtils;
import com.github.monet.controlserver.Parameter.BooleanParameter;
import com.github.monet.controlserver.Parameter.BoundedDecimalParameter;
import com.github.monet.controlserver.Parameter.BoundedIntegerParameter;
import com.github.monet.controlserver.Parameter.ChoiceParameter;
import com.github.monet.controlserver.Parameter.DecimalParameter;
import com.github.monet.controlserver.Parameter.IntegerParameter;
import com.github.monet.controlserver.Parameter.StringParameter;

/**
 * This class provides an iterator interface to query attributes that should be
 * set for a bundle.
 * <p>
 * Each step of the iterator resembles a dependency depth of parameters. So, if
 * for example you have a parameter that decides which algorithm to choose, then
 * any other parameter may be dependent upon that.
 */
public abstract class ParameterIterator implements Iterator<List<Parameter>>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -1323598391420240743L;
	public static String ALGORITHM_ROOT_ELEMENT = "algorithm";
	public static String GRAPH_ROOT_ELEMENT = "graph";

	/**
	 * Map containing all the set parameters.
	 */
	private Map<String, String> map;

	/**
	 * The current set of parameters.
	 */
	private Deque<NodeList> curParams;

	/**
	 * A map containing the subtree node list for the parameter and value
	 * selection.
	 */
	private Map<String, Map<String, NodeList>> choiceDescendants;

	/**
	 * The return value of <tt>getNext()</tt> stored to validate the parameters
	 * on execution of <tt>validateParameters</tt>.
	 */
	private List<Parameter> next;

	/**
	 * Desperate try to keep the stream open...
	 */
	private File xml;

	/**
	 * Indicator whether the elements of the
	 * <tt>next list have been updated by a call to <tt>validateParameters()</tt>
	 * </tt>.
	 */
	private boolean validated;

	/**
	 * The name field of the algorithm/graph.
	 */
	private String name;

	/**
	 * The description field of the algorithm/graph.
	 */
	private String description;

	/**
	 * Construct an algorithm parameter iterator for the given bundle.
	 *
	 * @param bundleDescriptor
	 *            the bundle descriptor string
	 * @throws ValidationException
	 * @throws BundleValidationException
	 */
	public ParameterIterator(final String rootName, String bundleDescriptor)
			throws BundleValidationException {
		this(rootName, bundleDescriptor, true);
	}

	ParameterIterator(final String rootName, String bundleDescriptor,
			boolean validate) throws BundleValidationException {
		ZipFile zip = null;
		try {
			File bundleFile = DependencyManager.getInstance().getFile(
					new BundleDescriptor(bundleDescriptor));
			zip = new ZipFile(bundleFile);
			boolean foundParametersFile = false;
			for (Enumeration<? extends ZipEntry> e = zip.entries(); e
					.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				if (entry.getName().equals("parameters.xml")) {
					init(rootName, zip.getInputStream(entry), validate);
					foundParametersFile = true;
					break;
				}
			}
			if (!foundParametersFile) {
				throw new BundleValidationException(
						"No parameters.xml in the bundle");
			}
		} catch (IOException | SAXException | ParserConfigurationException
				| ValidationException e) {
			throw new BundleValidationException(e.getMessage(), e);
		} finally {
			try {
				if (zip != null) {
					zip.close();
				}
			} catch (IOException e) {
				// The obligatory catch block.
				e.printStackTrace();
			}
		}
	}

	/**
	 * Construct an algorithm parameter iterator for the given bundle file.
	 *
	 * @param bundleFile
	 *            the bundle file
	 * @throws BundleValidationException
	 */
	public ParameterIterator(final String rootName, File bundleFile)
			throws BundleValidationException {
		this(rootName, bundleFile, true);
	}

	ParameterIterator(final String rootName, File bundleFile, boolean validate)
			throws BundleValidationException {
		if (bundleFile.getName().endsWith(".xml")) {
			try {
				init(rootName, new FileInputStream(bundleFile), true);
			} catch (ValidationException | SAXException | IOException
					| ParserConfigurationException e) {
				throw new BundleValidationException(e.getMessage(), e);
			}
		} else {
			ZipFile zip = null;
			boolean foundParametersFile = false;
			try {
				zip = new ZipFile(bundleFile);
				for (Enumeration<? extends ZipEntry> e = zip.entries(); e
						.hasMoreElements();) {
					ZipEntry entry = e.nextElement();
					if (entry.getName().equals("parameters.xml")) {
						foundParametersFile = true;
						init(rootName, zip.getInputStream(entry), validate);
						break;
					}
				}
			} catch (IOException | ValidationException | SAXException
					| ParserConfigurationException e) {
				throw new BundleValidationException(e.getMessage(), e);
			} finally {
				if (!foundParametersFile) {
					throw new BundleValidationException(
							"Could not find parameters.xml in bundle jar.");
				}
				try {
					if (zip != null) {
						zip.close();
					}
				} catch (IOException e) {
					// The obligatory catch block.
					e.printStackTrace();
				}
			}
		}
	}

	public ParameterIterator(final String rootName, InputStream xml)
			throws BundleValidationException {
		this(rootName, xml, true);
	}

	ParameterIterator(final String rootName, InputStream xml, boolean validate)
			throws BundleValidationException {
		try {
			init(rootName, xml, validate);
		} catch (ValidationException | SAXException | IOException
				| ParserConfigurationException e) {
			throw new BundleValidationException(e.getMessage(), e);
		}
	}

	/**
	 * Common initialization method for the different constructors.
	 *
	 * @param xmlis
	 *            the xml file as an input stream
	 * @param validate
	 *            validate the given file
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws ValidationException
	 */
	private void init(String rootName, InputStream xmlis, boolean validate)
			throws SAXException, IOException, ParserConfigurationException,
			ValidationException {
		choiceDescendants = new TreeMap<>();

		/* The file will be deleted if hasNext() returns false. */
		xml = FileUtils.createTempFileFromStream("parameters", ".xml", xmlis);
		xmlis.close();

		URL xmlSchema = Thread.currentThread().getContextClassLoader()
				.getResource("bundle-description.xsd");
		if (xmlSchema == null) {
			xmlSchema = ClassLoader.getSystemResource("bundle-description.xsd");
		}
		if (xmlSchema == null) {
			xmlSchema = getClass().getResource("bundle-description.xsd");
		}
		SchemaFactory schemaFactory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(xmlSchema);
		// Validator validator = schema.newValidator();
		// Source xmlFile = new StreamSource(xml);
		// validator.validate(xmlFile);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setSchema(schema);
		dbFactory.setValidating(false);
		dbFactory.setIgnoringComments(true);
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xml);
		doc.getDocumentElement().normalize();
		if (!doc.getDocumentElement().getNodeName().equals(rootName)) {
			throw new ValidationException(String.format(
					"No %s root node in algorithm.xml!", rootName));
		}
		NodeList curParamNodeList = doc.getChildNodes().item(0).getChildNodes();
		for (int i = 0; i < curParamNodeList.getLength(); i++) {
			Node n = curParamNodeList.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				switch (n.getNodeName()) {
				case "parameters":
					curParamNodeList = e.getChildNodes();
					break;
				case "name":
					name = e.getTextContent();
					break;
				case "description":
					description = e.getTextContent();
					break;
				case "instances":
				default:
					break;
				}
			}
		}
		validated = true;
		curParams = new LinkedList<>();
		curParams.add(curParamNodeList);
		map = new TreeMap<>();
	}

	@Override
	public synchronized boolean hasNext() {
		return curParams.size() > 0;
	}

	@Override
	public synchronized List<Parameter> next() {
		if (!validated) {
			throw new RuntimeException(
					"Calling next() without validating the parameter list!");
		}
		NodeList curParamNodeList = curParams.removeFirst();

		next = new ArrayList<>(curParamNodeList.getLength());
		for (int i = 0; i < curParamNodeList.getLength(); i++) {
			Node n = curParamNodeList.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (n.getNodeName().equals("parameter")) {
					final String name = e.getAttribute("name");
					Parameter p = algorithmParameterFromNode(e, name);
					next.add(p);
				} else if (n.getNodeName().equals("instances")) {
					Parameter p = createChoiceParameter(name, description, e,
							e.getAttribute("default"));
					next.add(p);
				}
			}
		}
		next = Collections.unmodifiableList(next);
		validated = false;
		return next;
	}

	private Parameter algorithmParameterFromNode(Element param, String name) {
		String desc = null;
		NodeList paramFields = param.getChildNodes();
		for (int k = 0; k < paramFields.getLength(); k++) {
			Node m = paramFields.item(k);
			if (m.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) m;
				switch (m.getNodeName()) {
				case "description":
					desc = e.getTextContent();
					break;
				case "boundedinteger":
					return new BoundedIntegerParameter(name, desc,
							e.getAttribute("default"), e.getAttribute("min"),
							e.getAttribute("minIsInclusive"),
							e.getAttribute("max"),
							e.getAttribute("maxIsInclusive"));
				case "boundeddecimal":
					return new BoundedDecimalParameter(name, desc,
							e.getAttribute("default"), e.getAttribute("min"),
							e.getAttribute("minIsInclusive"),
							e.getAttribute("max"),
							e.getAttribute("maxIsInclusive"));
				case "integer":
					return new IntegerParameter(name, desc,
							e.getAttribute("default"));
				case "boolean":
					return new BooleanParameter(name, desc,
							e.getAttribute("default"));
				case "string":
					return new StringParameter(name, desc,
							e.getAttribute("default"));
				case "decimal":
					return new DecimalParameter(name, desc,
							e.getAttribute("default"));
				case "choices":
				case "instances":
					return createChoiceParameter(name, desc, e,
							e.getAttribute("default"));
				default:
					break;
				}
			}
		}
		throw new RuntimeException("No elements in parameter node.");
	}

	private ChoiceParameter createChoiceParameter(final String name,
			final String description, final Element choice,
			final String defaultChoice) {
		List<String> choices = new LinkedList<>();
		Map<String, NodeList> choiceNodes = new TreeMap<>();
		NodeList children = choice.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getNodeName().equals("choice")
						|| e.getNodeName().equals("instance")) {
					String val = e.getAttribute("value");
					choices.add(val);
					choiceNodes.put(val, e.getChildNodes());
				}
			}
		}
		choiceDescendants.put(name, choiceNodes);
		return new ChoiceParameter(name, description, defaultChoice,
				Collections.unmodifiableList(choices));
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(
				"Removing parameters is not wise!");
	}

	/**
	 * Assume all elements of the previous next list were set and update the
	 * parameter map.
	 *
	 * @throws ParameterExceptioen
	 *             if a parameter has an invalid value.
	 */
	public void validateParameters() throws ParameterException {
		validateParameters(false);
	}

	void validateParameters(boolean lazy) throws ParameterException {
		if (!lazy) {
			validate(next);
		}
		for (Parameter p : next) {
			if (!p.isAssigned()) {
				continue;
			}
			switch (p.getType()) {
			case BOOLEAN:
			case BOUNDEDDECIMAL:
			case BOUNDEDINTEGER:
			case DECIMAL:
			case INTEGER:
			case SPACELESSSTRING:
			case STRING:
				map.put(p.getName(), p.getValue());
				break;
			case CHOICE:
				ChoiceParameter choice = (ChoiceParameter) p;
				map.put(choice.getName(), choice.getValue());
				NodeList tmp = choiceDescendants.get(choice.getName()).get(
						choice.getValue());
				if (tmp != null) {
					curParams.add(tmp);
				}
				choiceDescendants.remove(choice.getName());
				break;
			default:
				throw new RuntimeException("Unsupported parameter type: "
						+ p.getType().toString());
			}
		}
		validated = true;
	}

	/**
	 * Accumulate a single exception from the validation exceptions thrown by
	 * each call to the <tt>verify</tt> method of the parameter.
	 *
	 * @param parameters
	 *            the parameters to check
	 * @throws ParameterException
	 * @see {@link Parameter#verify()}
	 * @see ParameterException
	 */
	static void validate(Collection<Parameter> parameters)
			throws ParameterException {
		final List<Parameter> faulty = new LinkedList<>();
		for (Parameter p : parameters) {
			try {
				p.verify();
			} catch (BundleValidationException e) {
				faulty.add(p);
			}
		}
		if (faulty.size() > 0) {
			throw new ParameterException(Collections.unmodifiableList(faulty));
		}
	}

	/**
	 * This method throws a <tt>RuntimeException</tt> if there are still
	 * parameters left to set.
	 *
	 * @return the parameter map after iterating over all parameter selections
	 */
	public Map<String, String> getParameterMap() {
		if (hasNext()) {
			throw new RuntimeException("Not all parameters have been set!");
		}
		return map;
	}

	/**
	 * @return the name of the bundle
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the description of the bundle
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Close all resources allocated by this iterator.
	 */
	public void close() {
		if (xml != null) {
			xml.delete();
		}
	}

	/**
	 * @return the temporarily created XML file.
	 */
	File getTemporaryXMLFile() {
		return xml;
	}

}
