/**
 *
 */
package com.github.monet.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Repetitive stuff for files.
 */
public class FileUtils {
	private FileUtils() {
		// utility class
	}

	/**
	 * Create a temporary file from a given input stream that will be deleted
	 * upon proper exit of the JVM.
	 *
	 * @param prefix
	 *            prefix of the file
	 * @param suffix
	 *            suffix of the file
	 * @param is
	 *            the input stream to read the file's contents from
	 * @return a File object for the newly created file
	 */
	public static File createTempFileFromStream(String prefix, String suffix,
			InputStream is) {
		FileOutputStream fos = null;
		try {
			File tempFile = File.createTempFile(prefix, suffix);
			fos = new FileOutputStream(tempFile);
			byte[] buffer = new byte[4096];

			int read = 0;
			while ((read = is.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			tempFile.deleteOnExit();
			return tempFile;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
