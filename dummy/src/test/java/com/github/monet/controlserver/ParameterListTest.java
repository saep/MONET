/**
 *
 */
package com.github.monet.controlserver;

import static org.junit.Assert.*;

import java.util.List;
import java.io.InputStream;

import org.junit.Test;

import com.github.monet.common.BundleValidationException;
import com.github.monet.controlserver.AlgorithmParameterList;
import com.github.monet.controlserver.Parameter;
import com.github.monet.controlserver.ParameterException;
import com.github.monet.controlserver.ParameterList;

public class ParameterListTest {
	@Test
	public void testDummyParameterList() throws BundleValidationException,
			ParameterException {
		InputStream xmlis = ClassLoader
				.getSystemResourceAsStream("parameters.xml");
		ParameterList pl = new AlgorithmParameterList(xmlis);

		int i = 0;
		for (; i < 2; i++) {
			List<Parameter> params = pl.getParameterList();

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
			pl.validateParameters();
		}
		assertEquals(2, i);
	}

}
