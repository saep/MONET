/*
 * This class is too trivial to license.
 */
package com.github.monet.common;

public class MongoBuilderException extends RuntimeException {
	private static final long serialVersionUID = 6058848056113201995L;

	private String shortMessage;
	private String path;
	private String token;
	private int position;

	public MongoBuilderException(String shortMessage, String path,
			String token, int position) {
		super();
		this.shortMessage = shortMessage;
		this.path = path;
		this.token = token;
		this.position = position;
	}

	public String getMessage() {
		return String.format(
				"Irregular path '%s': %s; position: %d, in token '%s'", path,
				shortMessage, position, token);
	}

	public String getShortMessage() {
		return shortMessage;
	}

	public String getPath() {
		return path;
	}

	public String getToken() {
		return token;
	}

	public int getPosition() {
		return position;
	}

}
