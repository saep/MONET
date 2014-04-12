/*
 * This interface is too trivial to license.
 */
package com.github.monet.interfaces;

import java.io.IOException;

/**
 * A stream that can be used to write arbitrary string data.
 *
 * @author Max Günther
 * @see MongoMeasurementStream, TestMeasurementStream
 *
 */
public interface MeasurementStream {

	/**
	 * Starts a new section in the file.
	 *
	 * Appends two newlines, 20 "=", a space, the <code>name</code, a space, 20
	 * "=" and a newline to the file.
	 */
	public void startSection(String name);

	/**
	 * Writes a String to the MeasurementStream.
	 *
	 * @param str
	 */
	public void write(String str);

	/**
	 * Saves the file to GridFS
	 *
	 * @throws IOException
	 */
	public void saveFile();

}
