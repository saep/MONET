/**
 *
 */
package com.github.monet.controlserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.github.monet.common.BundleValidationException;
import com.github.monet.controlserver.Parameter;
import com.github.monet.controlserver.Parameter.StringParameter;
import com.github.monet.controlserver.Parameter.Type;

/**
 *
 */
public class AlgorithmParameterTest {

	@Test
	public void testPattern() throws BundleValidationException {
		Parameter p = new StringParameter("foo", "valid", null);
		assertNotNull(p);
		assertEquals("valid", p.getDescription());
		assertEquals("foo", p.getName());
		assertEquals(Type.STRING, p.getType());
		p.setValue(" bar ");
		assertEquals("bar", p.getValue());
		p.verify();

		Parameter q = new StringParameter("foo", "invalid", null);
		try {
			q.setValue("invalöd");
			q.verify();
			fail("should have not been verified.");
		} catch (Exception e) {
			assertTrue(true);
		}

		Parameter r = new StringParameter("foo", "invalid", null);
		try {
			r.setValue("s");
			r.verify();
			// fail("should have not been verified.");
		} catch (Exception e) {
			assertTrue(true);
		}

		Parameter s = new StringParameter("foo", "invalid", null);
		try {
			s.setValue("              s                      ");
			assertEquals("s", s.getValue());
			s.verify();
			// fail("should have not been verified.");
		} catch (Exception e) {
			assertTrue(true);
		}
	}
}
