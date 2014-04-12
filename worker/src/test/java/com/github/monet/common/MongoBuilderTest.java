package com.github.monet.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.monet.common.Config;
import com.github.monet.common.MongoBuilder;
import com.github.monet.common.MongoBuilderException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class MongoBuilderTest {
	private static DBCollection coll;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		coll = Config.createTestDB().getCollection("MongoBuilderTest");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (coll != null) {
			coll.drop();
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMap1() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("abc", 3);
		builder.insert("def", 4);
		assertEquals("{ \"abc\" : 3 , \"def\" : 4}", builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testMap2() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("abc/asd", 3);
		builder.insert("abc/fgh", 4);
		builder.insert("def/asd", 6);
		builder.insert("def/fgh", 7);
		assertEquals(
				"{ \"abc\" : { \"asd\" : 3 , \"fgh\" : 4} , \"def\" : { \"asd\" : 6 , \"fgh\" : 7}}",
				builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testMap3() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("one/a/Xone", 1);
		builder.insert("one/a/Xtwo", 2);
		builder.insert("one/b/Xone", 3);
		builder.insert("one/b/Xtwo", 4);
		builder.insert("two/a", 5);
		builder.insert("two/b", 6);
		assertEquals(
				"{ \"one\" : { \"a\" : { \"Xone\" : 1 , \"Xtwo\" : 2} , \"b\" : { \"Xone\" : 3 , \"Xtwo\" : 4}} , \"two\" : { \"a\" : 5 , \"b\" : 6}}",
				builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testList1() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("one/#", 1);
		builder.insert("one/#", 2);
		assertEquals("{ \"one\" : [ 1 , 2]}", builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testList2() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("one/#", 1);
		builder.insert("one/#0", 2);
		assertEquals("{ \"one\" : [ 1]}", builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testList3() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("one/#/asd", 1);
		builder.insert("one/#/asd", 2);
		assertEquals("{ \"one\" : [ { \"asd\" : 1} , { \"asd\" : 2}]}",
				builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testList4() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("one/#/asd", 1);
		builder.insert("one/#0/def", 2);
		assertEquals("{ \"one\" : [ { \"asd\" : 1 , \"def\" : 2}]}",
				builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testList5() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("one/#0/asd", 1);
		builder.insert("one/#0/def", 2);
		assertEquals("{ \"one\" : [ { \"asd\" : 1 , \"def\" : 2}]}",
				builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testNoHashStart() {
		MongoBuilder builder = new MongoBuilder();
		try {
			builder.insert("#/asd", 1);
			fail("should throw MongoBuilderException");
		} catch (MongoBuilderException ex) {
			assertEquals("may not start with '#'", ex.getShortMessage());
			assertEquals("#/asd", ex.getPath());
			assertEquals("#", ex.getToken());
			assertEquals(0, ex.getPosition());
		}
	}

	@Test
	public void testListOutOfBounds1() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("asd/#", 1);
		builder.insert("asd/#1", 3);
		try {
			builder.insert("asd/#4", 4);
			fail("should throw MongoBuilderException");
		} catch (MongoBuilderException ex) {
			assertEquals("index of of bounds", ex.getShortMessage());
			assertEquals("asd/#4", ex.getPath());
			assertEquals("#4", ex.getToken());
			assertEquals(4, ex.getPosition());
		}
	}

	@Test
	public void testListOutOfBounds2() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("asd/#/a", 1);
		builder.insert("asd/#1/c", 4);
		try {
			builder.insert("asd/#3/d", 4);
			fail("should throw MongoBuilderException");
		} catch (MongoBuilderException ex) {
			assertEquals("index of of bounds", ex.getShortMessage());
			assertEquals("asd/#3/d", ex.getPath());
			assertEquals("#3", ex.getToken());
			assertEquals(4, ex.getPosition());
		}
	}

	@Test
	public void testStartSlash() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("/abc", 3);
		builder.insert("/def", 4);
		assertEquals("{ \"abc\" : 3 , \"def\" : 4}", builder.toString());
		coll.insert(builder);
	}

	@Test
	public void testMixedListKey() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("abc/#/a", 3);
		try {
			builder.insert("abc/a", 4);
		} catch (MongoBuilderException ex) {
			assertEquals("expected list but got key", ex.getShortMessage());
			assertEquals("abc/a", ex.getPath());
			assertEquals("a", ex.getToken());
			assertEquals(4, ex.getPosition());
		}
	}

	@Test
	public void testMixedKeyList() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("abc/a", 4);
		try {
			builder.insert("abc/#/a", 3);
		} catch (MongoBuilderException ex) {
			assertEquals("expected key but got list", ex.getShortMessage());
			assertEquals("abc/#/a", ex.getPath());
			assertEquals("#", ex.getToken());
			assertEquals(4, ex.getPosition());
		}
	}

	@Test
	public void testFind1() {
		MongoBuilder builder = new MongoBuilder();
		builder.insert("one/a/Xone", 1);
		builder.insert("one/a/Xtwo", 2);
		builder.insert("one/b/Xone", 3);
		builder.insert("one/b/Xtwo", 4);
		builder.insert("two/a", 5);
		builder.insert("two/b", 6);
		builder.insert("three/#/asd", 7);
		builder.insert("three/#/asd", 8);
		builder.insert("three/#0/def", 9);
		assertEquals(1, MongoBuilder.find(builder, "one/a/Xone"));
		assertEquals(2, MongoBuilder.find(builder, "one/a/Xtwo"));
		assertEquals(3, MongoBuilder.find(builder, "one/b/Xone"));
		assertEquals(4, MongoBuilder.find(builder, "one/b/Xtwo"));
		assertEquals(5, MongoBuilder.find(builder, "two/a"));
		assertEquals(6, MongoBuilder.find(builder, "two/b"));
		assertEquals(7, MongoBuilder.find(builder, "three/#0/asd"));
		assertEquals(8, MongoBuilder.find(builder, "three/#1/asd"));
		assertEquals(9, MongoBuilder.find(builder, "three/#0/def"));

		builder.put("_id", 1);
		coll.insert(builder);

		DBObject found = coll.findOne(new BasicDBObject("_id", 1));
		assertEquals(9, MongoBuilder.find(found, "three/#0/def"));
	}

}
