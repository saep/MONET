/*
 * Copyright (C) 2013,2014
 * Jakob Bossek, Michael Capelle, Hendrik Fichtenberger, Max Günther, Johannes
 * Kowald, Marco Kuhnke, David Mezlaf, Christopher Morris, Andreas Pauly, Sven
 * Selmke and Sebastian Witte
 *
 *  This class is part of MONET.
 *
 *  This class is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This class is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with MONET.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.monet.common;

import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Implements a simple way to build MongoDB objects.
 *
 * This class doesn't hold any additional properties or internal state compared
 * to its ancestor {@link BasicDBObject}, so you can cast BasicDBObject to
 * MongoBuilder.
 *
 * <pre>
 * <code>
 * MongoBuilder builder = new MongoBuilder();
 * builder.insert("foo/bar", 1);
 * builder.insert("leet/#", 2);
 * coll.insert(builder);
 * </code>
 * </pre>
 *
 * @author Max Günther
 *
 */
public class MongoBuilder extends BasicDBObject {
	private static final long serialVersionUID = 8906097484838488572L;

	public MongoBuilder() {
		super();
	}

	/**
	 * Finds a value or intermediate list or map in an {@link DBObject} using a
	 * path expression.
	 *
	 * @param dbobj
	 *            the MongoDB database object to look in
	 * @param path
	 *            a path expression
	 * @return a the found value
	 * @throws MongoBuilderException
	 *             if the path was malformed or conflicted with the dbobj.
	 */
	@SuppressWarnings("unchecked")
	public static Object find(DBObject dbobj, String path) {
		// ignore leading slash
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		// initialize
		Tokenizer tokenizer = new Tokenizer(path);
		Object current = dbobj;
		Token currentToken = tokenizer.nextToken();

		// can't start with '#'
		if (currentToken.startsWith("#")) {
			throw new MongoBuilderException("may not start with '#'", path,
					currentToken.toString(), currentToken.pos);
		}

		// go through all tokens
		while (currentToken != null) {
			if (current instanceof Map) {
				// it's a map so the current token needs to address a map
				if (currentToken.startsWith("#")) {
					throw new MongoBuilderException(
							"expected key but got list", path,
							currentToken.token, currentToken.pos);
				}
				Map<String, Object> currentAsMap = (Map<String, Object>) current;
				current = currentAsMap.get(currentToken.toString());
			} else if (current instanceof List) {
				// it's a list so the current token needs to address a list
				if (!currentToken.startsWith("#")) {
					throw new MongoBuilderException(
							"expected list but got key", path,
							currentToken.token, currentToken.pos);
				}
				List<Object> currentAsList = (List<Object>) current;

				// find and parse a potential index
				Integer index = null;
				if (currentToken.length() > 1) {
					index = currentToken.asIndex();
					if (index >= currentAsList.size()) {
						throw new MongoBuilderException("index out of bounds",
								path, currentToken.token, currentToken.pos);
					}
				} else {
					index = currentAsList.size() - 1;
				}
				current = currentAsList.get(index);
			}
			currentToken = tokenizer.nextToken();
		}
		return current;
	}

	/**
	 * Insert into the BasicDBObject using a path expression.
	 *
	 * @param path
	 *            where to insert
	 * @param value
	 *            the value to insert
	 * @return
	 * @throws MongoBuilderException
	 *             if the path is illegally formed or conflicts with previous
	 *             inserts
	 */
	@SuppressWarnings("unchecked")
	public BasicDBObject insert(String path, Object value) {
		// ignore leading slash
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		// initialize
		Tokenizer tokenizer = new Tokenizer(path);
		Object current = this;
		Object newCurrent = null;
		Token currentToken = tokenizer.nextToken();
		Token nextToken = tokenizer.nextToken();

		// can't start with '#'
		if (currentToken.startsWith("#")) {
			throw new MongoBuilderException("may not start with '#'", path,
					currentToken.toString(), currentToken.pos);
		}

		// go through all tokens
		while (currentToken != null) {
			if (current instanceof Map) {
				// it's a map so the current token needs to address a map
				if (currentToken.startsWith("#")) {
					throw new MongoBuilderException(
							"expected key but got list", path,
							currentToken.token, currentToken.pos);
				}
				Map<String, Object> currentAsMap = (Map<String, Object>) current;
				newCurrent = currentAsMap.get(currentToken.toString());
				if (newCurrent == null) {
					// need to insert an object according to the next token
					if (nextToken == null) {
						// no next token; insert value
						currentAsMap.put(currentToken.toString(), value);
					} else if (nextToken.startsWith("#")) {
						// next token is a list; insert list
						newCurrent = new BasicDBList();
						currentAsMap.put(currentToken.toString(), newCurrent);
					} else {
						// next token is a map; insert map
						newCurrent = new BasicDBObject();
						currentAsMap.put(currentToken.toString(), newCurrent);
					}
				}
			} else if (current instanceof List) {
				// it's a list so the current token needs to address a list
				if (!currentToken.startsWith("#")) {
					throw new MongoBuilderException(
							"expected list but got key", path,
							currentToken.token, currentToken.pos);
				}
				List<Object> currentAsList = (List<Object>) current;
				newCurrent = null;

				// find and parse a potential index
				Integer index = null;
				if (currentToken.length() > 1) {
					index = currentToken.asIndex();
					if (index == currentAsList.size()) {
						index = null;
					} else {
						try {
							newCurrent = currentAsList.get(index);
						} catch (IndexOutOfBoundsException ex) {
							throw new MongoBuilderException(
									"index of of bounds", path,
									currentToken.token, currentToken.pos);
						}
					}
				}

				if (newCurrent == null) {
					// there was nothing at given index we need to add according
					// to the next token
					Object toAdd;
					if (nextToken == null) {
						// no next token; insert value
						toAdd = value;
					} else if (nextToken.startsWith("#")) {
						// next token is a list; add a list
						newCurrent = new BasicDBList();
						toAdd = newCurrent;
					} else {
						// next token is a map; add a map
						newCurrent = new BasicDBObject();
						toAdd = newCurrent;
					}
					// add or insert
					if (index == null) {
						currentAsList.add(toAdd);
					} else {
						try {
							currentAsList.add(index, toAdd);
						} catch (IndexOutOfBoundsException ex) {
							throw new MongoBuilderException(
									"index of of bounds", path,
									currentToken.token, currentToken.pos);
						}
					}
				}
			}

			// step forward
			current = newCurrent;
			currentToken = nextToken;
			nextToken = tokenizer.nextToken();
		}

		return this;
	}

	/**
	 * Clear all inserted data.
	 */
	@Override
	public void clear() {
		super.clear();
	}

	private static class Tokenizer {
		private String input;
		private int pos = 0;

		public Tokenizer(String input) {
			super();
			this.input = input;
		}

		private Token nextToken() {
			int i = pos;
			for (; i < input.length(); i++) {
				if (input.charAt(i) == '/') {
					Token ret = new Token(input.substring(pos, i), pos);
					i++;
					pos = i;
					return ret;
				}
			}
			Token ret = new Token(input.substring(pos, i), pos);
			pos = input.length();
			if (ret.token.equals(""))
				return null;
			else
				return ret;
		}
	}

	private static class Token {
		private String token;
		private int pos;

		public Token(String token, int pos) {
			super();
			this.token = token;
			this.pos = pos;
		}

		@Override
		public String toString() {
			return token;
		}

		public boolean startsWith(String prefix) {
			return token.startsWith(prefix);
		}

		public int length() {
			return token.length();
		}

		public int asIndex() {
			return Integer.parseInt(token.substring(1, token.length()));
		}
	}

}
