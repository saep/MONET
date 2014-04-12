/**
 *
 */
package com.github.monet.controlserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.monet.common.BundleValidationException;
import com.github.monet.controlserver.AlgorithmParameterIterator;
import com.github.monet.controlserver.Parameter;
import com.github.monet.controlserver.ParameterException;
import com.github.monet.controlserver.ParameterIterator;
import com.github.monet.controlserver.ParserParameterIterator;
import com.github.monet.controlserver.Parameter.ChoiceParameter;

/**
 *
 */
public class ParameterIteratorTest {

	@Test
	public void testCorrectParameters() throws BundleValidationException,
			IOException {
		InputStream xmlis = ClassLoader
				.getSystemResourceAsStream("parameters.xml");
		ParameterIterator it = new AlgorithmParameterIterator(xmlis);

		/* Helper variable to store recursion depth in the xml dom tree. */
		int depth = 0;

		while (it.hasNext()) {
			List<Parameter> params = it.next();

			switch (depth) {
			case 0:
				assertEquals(8, params.size());
				for (Parameter p : params) {
					switch (p.getName()) {
					case "bla":
						p.setValue("SSSP");
						p.verify();
						assertEquals("SSSP", p.getValue());
						break;
					case "field_of_study":
						p.setValue("Quantenphysik");
						p.verify();
						assertEquals("Quantenphysik", p.getValue());
						break;
					case "popsize":
						p.setValue(" 2   ");
						assertEquals("2", p.getValue());
						assertEquals("1", p.defaultValue());
						break;
					default:
						p.setValue(null);
						assertNotNull(p.getValue());
						assertEquals(p.defaultValue(), p.getValue());
					}
				}
				break;
			case 1:
				assertEquals(2, params.size());
				for (Parameter p : params) {
					switch (p.getName()) {
					case "sssp_specific_parameter1":
						assertEquals("42", p.defaultValue());
						p.setValue(null);
						assertEquals(p.defaultValue(), p.getValue());
						break;
					case "sssp_specific_parameter2":
						assertEquals("4711", p.defaultValue());
						p.setValue(null);
						assertEquals(p.defaultValue(), p.getValue());
						break;
					default:
						fail("Unexpected parameter name!");
					}
				}
				break;
			case 2:
				assertEquals(0, params.size());
				break;
			default:
				fail("Reached unvalidated block in switch case statement.");
				break;
			}
			try {
				it.validateParameters();
			} catch (ParameterException e) {
				e.printStackTrace();
				fail("This test should have worked.");
			}

			/* Finally increase the depth for the next step. */
			depth++;
		}
		assertEquals(3, depth);
		Map<String, String> m = it.getParameterMap();
		assertEquals("SSSP", m.get("bla"));
		assertEquals("true", m.get("launch_a_nuke"));
		assertEquals("3.14159", m.get("pi"));
		assertEquals("2", m.get("popsize"));
		assertEquals("0.5", m.get("probability"));
		assertEquals("1", m.get("seed"));
		assertEquals("42", m.get("sssp_specific_parameter1"));
		assertEquals("4711", m.get("sssp_specific_parameter2"));
		assertEquals("Quantenphysik", m.get("field_of_study"));
		assertEquals(10, m.size());
		xmlis.close();
	}

	@Test
	public void testGraph() throws BundleValidationException {
		InputStream xmlis = ClassLoader
				.getSystemResourceAsStream("example-graph.xml");
		ParameterIterator it = new ParserParameterIterator(xmlis);
		assertEquals("Cool_Graph", it.getName());
		assertEquals("This graph is really cool, it can do magic things!",
				it.getDescription());
		int depth = 0;
		while (it.hasNext()) {
			List<Parameter> current = it.next();

			switch (depth) {
			case 0:
				assertEquals(1, current.size());
				assertTrue(current.get(0) instanceof ChoiceParameter);
				ChoiceParameter instance = (ChoiceParameter) current.get(0);
				assertEquals(2, instance.getChoices().size());
				assertEquals("SSSP", instance.getChoices().get(0));
				assertEquals("MST", instance.getChoices().get(1));
				break;
			default:
				fail("Unknown parameter depth.");
			}
			depth++;
		}
		assertEquals(1, depth);
	}
}
