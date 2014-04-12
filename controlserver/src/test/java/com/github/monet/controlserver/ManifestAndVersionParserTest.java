/**
 *
 */
package com.github.monet.controlserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.monet.common.BundleValidationException;
import com.github.monet.common.BundleVersion;
import com.github.monet.common.VersionedPackage;
import com.github.monet.common.BundleVersion.Kind;
import com.github.monet.controlserver.ManifestParser;

/**
 * The tests in this class assume that the dummy algorithm bundle is present.
 */
public class ManifestAndVersionParserTest {
	private static ManifestParser manifestParser;
	private static File dummy;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("monet_dummy_algorithm-0.0.1-SNAPSHOT.jar");
		dummy = File.createTempFile("Bundle", ".jar");
		FileOutputStream fos = new FileOutputStream(dummy);
		byte[] buffer = new byte[2048];
		int c;

		while ((c = is.read(buffer)) > 0) {
			fos.write(buffer, 0, c);
		}
		JarFile dummyJar = new JarFile(dummy);
		manifestParser = new ManifestParser(dummyJar);
		is.close();
		fos.close();
		dummyJar.close();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		dummy.delete();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testVersionDetection() {
		String exclusiveMinBound = "(1.0";
		String exclusiveMaxBound = "2.4)";
		String inclusiveMinBound = "[7.1-FOO";
		String inclusiveMaxBound = "123.1515.1215151-SNAPSHOT]";
		String range = "(1,2)";
		String simple = "1.3.6";
		String notSoSimple = "1.3.6-INSANELYDIFFICULT";
		String stupidRange = "(2,1)";
		String any = "";
		try {
			BundleVersion exclusiveMinBoundVersion = BundleVersion
					.fromString(exclusiveMinBound);
			assertTrue(exclusiveMinBoundVersion.kind() == Kind.LOWERBOUND);
			assertFalse(exclusiveMinBoundVersion.lowerBoundIsInclusive());
			assertEquals(exclusiveMinBound, exclusiveMinBoundVersion.show());

			BundleVersion exclusiveMaxBoundVersion = BundleVersion
					.fromString(exclusiveMaxBound);
			assertTrue(exclusiveMaxBoundVersion.kind() == Kind.UPPERBOUND);
			assertFalse(exclusiveMinBoundVersion.upperBoundIsInclusive());
			assertEquals(exclusiveMaxBound, exclusiveMaxBoundVersion.show());

			BundleVersion inclusiveMinBoundVersion = BundleVersion
					.fromString(inclusiveMinBound);
			assertTrue(inclusiveMinBoundVersion.kind() == Kind.LOWERBOUND);
			assertTrue(inclusiveMinBoundVersion.lowerBoundIsInclusive());
			assertEquals(inclusiveMinBound, inclusiveMinBoundVersion.show());

			BundleVersion inclusiveMaxBoundVersion = BundleVersion
					.fromString(inclusiveMaxBound);
			assertTrue(inclusiveMaxBoundVersion.kind() == Kind.UPPERBOUND);
			assertTrue(inclusiveMaxBoundVersion.upperBoundIsInclusive());
			assertEquals(inclusiveMaxBound, inclusiveMaxBoundVersion.show());

			BundleVersion rangeVersion = BundleVersion.fromString(range);
			assertFalse(rangeVersion.lowerBoundIsInclusive());
			assertFalse(rangeVersion.upperBoundIsInclusive());
			assertEquals(range, rangeVersion.show());

			BundleVersion simpleVersion = BundleVersion.fromString(simple);
			assertTrue(simpleVersion.lowerBoundIsInclusive());
			assertTrue(simpleVersion.upperBoundIsInclusive());
			assertEquals(simple, simpleVersion.show());

			BundleVersion notSoSimpleVersion = BundleVersion
					.fromString(notSoSimple);
			assertTrue(notSoSimpleVersion.lowerBoundIsInclusive());
			assertTrue(notSoSimpleVersion.upperBoundIsInclusive());
			assertEquals(notSoSimple, notSoSimpleVersion.show());

			BundleVersion anyVersion = BundleVersion.fromString(any);
			assertFalse(anyVersion.lowerBoundIsInclusive());
			assertFalse(anyVersion.upperBoundIsInclusive());
			assertEquals(any, anyVersion.show());
		} catch (BundleValidationException e) {
			e.printStackTrace();
			fail("These strings should not have failed");
		}
		try {
			assertEquals(BundleVersion.getInvalidVersion(),
					BundleVersion.fromString(stupidRange));
		} catch (BundleValidationException e) {
			fail("Should have returned an InvalidVersion object.");
		}
	}

	/**
	 * Test method for
	 * {@link monet.controlserver.bundle.ManifestParser#parseList(java.lang.String)}
	 * .
	 */
	@Test
	public void testParseListEmpty() {
		String field = "Empty-Import-Package";
		Collection<Map<String, String>> empty;
		try {
			empty = manifestParser.parseList(field);
			assertTrue(empty.isEmpty());
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testFaultyVersionRangeMissingUpperBound() throws ParseException {
		String field = "FaultyVerisonRangeMissingUpperBound-Import-Package";
		Collection<Map<String, String>> list = manifestParser.parseList(field);
		assertEquals(1, list.size());
		for (Map<String, String> m : list) {
			assertNotNull(m.get("foobar"));
			final String version = m.get("version");
			assertNotNull(version);
			assertEquals("(1,1", version);
			try {
				BundleVersion.fromString(version);
				fail("Should have thrown an exception");
			} catch (BundleValidationException e) {
				// should jump here
			}
		}
	}

	@Test
	public void testFaultyVersion() throws ParseException {
		String field = "FaultyVersion-Import-Package";
		Collection<Map<String, String>> list = manifestParser.parseList(field);
		assertEquals(1, list.size());
		for (Map<String, String> m : list) {
			assertNotNull(m.get("foobar"));
			try {
				BundleVersion.fromString(m.get("version"));
				fail("The version has a wrong format.");
			} catch (BundleValidationException e) {
				// should jump here
			}
		}
	}

	@Test
	public void testFaultyVerisonRangeNoBounds() throws ParseException {
		String field = "FaultyVerisonRangeNoBounds-Import-Package";
		Collection<Map<String, String>> list = manifestParser.parseList(field);
		assertEquals(1, list.size());
		for (Map<String, String> m : list) {
			assertNotNull(m.get("foobar"));
			try {
				BundleVersion.fromString(m.get("version"));
				fail("The version has a wrong format.");
			} catch (BundleValidationException e) {
				// should jump here
			}
		}
	}

	@Test
	public void testFaultyVerisonRangeImpossibleBounds() throws ParseException,
			BundleValidationException {
		String field = "FaultyVerisonRangeImpossibleBounds-Import-Package";
		Collection<Map<String, String>> list = manifestParser.parseList(field);
		assertEquals(1, list.size());
		for (Map<String, String> m : list) {
			assertNotNull(m.get("foobar"));
			assertEquals(BundleVersion.getInvalidVersion(),
					BundleVersion.fromString(m.get("version")));
		}
	}

	@Test
	public void testProperAnyVersion() throws ParseException {
		String field = "ProperAnyVersion-Import-Package";
		Collection<Map<String, String>> line = manifestParser.parseList(field);
		assertEquals(1, line.size());
		for (Map<String, String> m : line) {
			assertEquals(1, m.size());
			assertNotNull(m.get("foobar"));
			assertNull(m.get("version"));
			try {
				BundleVersion.fromString(null);
				fail("The parser should have accepted this.");
			} catch (BundleValidationException e) {
				// all is well
			}
		}
	}

	@Test
	public void testProperVersionRange() throws ParseException {
		try {
			String field = "ProperVersionRange-Import-Package";
			Collection<Map<String, String>> line = manifestParser
					.parseList(field);
			assertEquals(1, line.size());
			for (Map<String, String> m : line) {
				assertEquals(2, m.size());
				assertNotNull(m.get("foobar"));
				String version = m.get("version");
				assertEquals("[1,2)", version);
				assertEquals(version, BundleVersion.fromString(version).show());
			}
		} catch (BundleValidationException e) {
			fail("The line is valid.");
			e.printStackTrace();
		}
	}

	@Test
	public void testProperVersionUpperBound() throws ParseException {
		try {
			String field = "ProperVersionUpperBound";
			Collection<Map<String, String>> line = manifestParser
					.parseList(field);
			for (Map<String, String> m : line) {
				assertEquals(1, m.size());
				String version = m.get("version");
				assertNotNull(version);
				assertEquals("7]", version);
				final BundleVersion bundleVersion = BundleVersion
						.fromString(version);
				assertEquals(version, bundleVersion.show());
				assertTrue(bundleVersion.upperBoundIsInclusive());
			}
		} catch (BundleValidationException e) {
			fail("The line is valid.");
			e.printStackTrace();
		}
	}

	@Test
	public void testProperVersionLowerBound() throws ParseException {
		try {
			String field = "ProperVersionLowerBound";
			Collection<Map<String, String>> line = manifestParser
					.parseList(field);
			for (Map<String, String> m : line) {
				assertEquals(1, m.size());
				String version = m.get("version");
				assertNotNull(version);
				assertEquals("[7", version);
				final BundleVersion bundleVersion = BundleVersion
						.fromString(version);
				assertEquals(version, bundleVersion.show());
				assertTrue(bundleVersion.lowerBoundIsInclusive());
			}
		} catch (BundleValidationException e) {
			fail("The line is valid.");
			e.printStackTrace();
		}

	}

	@Test
	public void testProperSingleVersionSimple() throws ParseException {
		try {
			String field = "ProperSingleVersionSimple";
			Collection<Map<String, String>> line = manifestParser
					.parseList(field);
			for (Map<String, String> m : line) {
				assertEquals(1, m.size());
				String version = m.get("version");
				assertNotNull(version);
				assertEquals("2.4", version);
				final BundleVersion bundleVersion = BundleVersion
						.fromString(version);
				assertEquals(version, bundleVersion.show());
				assertTrue(bundleVersion.lowerBoundIsInclusive());
			}
		} catch (BundleValidationException e) {
			fail("The line is valid.");
			e.printStackTrace();
		}
	}

	@Test
	public void testProperSingleVersionComplicated() throws ParseException {
		try {
			String field = "ProperSingleVersionComplicated";
			Collection<Map<String, String>> line = manifestParser
					.parseList(field);
			for (Map<String, String> m : line) {
				assertEquals(1, m.size());
				String version = m.get("version");
				assertNotNull(version);
				assertEquals("3.6.1-SNAPSHOT77", version);
				final BundleVersion bundleVersion = BundleVersion
						.fromString(version);
				assertEquals(version, bundleVersion.show());
				assertTrue(bundleVersion.lowerBoundIsInclusive());
			}
		} catch (BundleValidationException e) {
			fail("The line is valid.");
			e.printStackTrace();
		}
	}

	@Test
	public void testDummyToBundleDescriptor() throws ParseException,
			BundleValidationException {
		Set<VersionedPackage> exports = manifestParser.retrieveExports();
		Set<VersionedPackage> imports = manifestParser.retrieveImports();
		assertEquals(1, exports.size());
		assertTrue(imports.isEmpty());
	}

	@Test
	public void testMerge() throws BundleValidationException {
		BundleVersion single1337 = BundleVersion.fromString("1.33.7");
		BundleVersion single42 = BundleVersion.fromString("42");
		BundleVersion single042 = BundleVersion.fromString("042");
		BundleVersion incLower = BundleVersion.fromString("[1.0033.7");
		BundleVersion excLower = BundleVersion.fromString("(1.33.7");
		BundleVersion incUpper = BundleVersion.fromString("1.33.7]");
		BundleVersion excUpper = BundleVersion.fromString("1.33.7)");
		BundleVersion bigUpper = BundleVersion.fromString("2.33.7]");

		List<BundleVersion> allVersions = Arrays.asList(single42, single042,
				single1337, incLower, excLower, incUpper, excUpper, bigUpper);

		/* getInvalidVersion() should point to the same object */
		assertTrue(BundleVersion.getInvalidVersion() == BundleVersion
				.getInvalidVersion());

		/*
		 * Merging should create a new object unless it returns the any version
		 * or the invalid version singleton object.
		 */
		for (BundleVersion v : allVersions) {
			assertEquals(v.kind(), v.clone().kind());
			if (v == BundleVersion.getInvalidVersion()
					|| v == BundleVersion.getAnyVersion()) {
				continue;
			}
			assertEquals(v.show(), v.merge(v).show());
			assertFalse(v == v.merge(v));
			final BundleVersion vMergeAny = v.merge(BundleVersion
					.getAnyVersion());
			assertEquals(v.show(), vMergeAny.show());
			assertEquals(v.kind(), vMergeAny.kind());
			assertFalse(vMergeAny == v);
			final BundleVersion anyMergeV = BundleVersion.getAnyVersion()
					.merge(v);
			assertEquals(v.show(), anyMergeV.show());
			assertEquals(v.kind(), anyMergeV.kind());
			assertFalse(anyMergeV == v);

			/* Symmetry checks */
			for (BundleVersion w : allVersions) {
				assertEquals(v.merge(w).show(), w.merge(v).show());
			}
		}

		/* Merges that should return an invalid version object. */
		assertEquals(BundleVersion.getInvalidVersion(),
				single42.merge(single1337));
		assertEquals(BundleVersion.getInvalidVersion(),
				single1337.merge(single42));

		assertEquals(single42.show(), single042.show());

		assertEquals(single1337.show(), incUpper.merge(incLower).show());
		assertTrue(BundleVersion.getInvalidVersion() == incUpper
				.merge(excLower));
		assertTrue(BundleVersion.getInvalidVersion() == excUpper
				.merge(excLower));
		assertTrue(BundleVersion.getInvalidVersion() == incLower
				.merge(excUpper));

		final BundleVersion range = bigUpper.merge(incLower);
		assertTrue(range.upperBoundIsInclusive());
		assertTrue(range.lowerBoundIsInclusive());
		assertEquals(range.kind(), BundleVersion.Kind.RANGE);
		assertEquals(incLower.show() + "," + bigUpper.show(), range.show());
	}

}
