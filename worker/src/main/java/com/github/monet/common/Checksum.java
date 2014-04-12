package com.github.monet.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class that stores static functions to calculate or operate with hash
 * sums.
 */
public class Checksum {
	/**
	 * Calculate the sha256 sum of the given file.
	 * <p>
	 *
	 * @param f
	 *            the input file
	 * @return a String containing the check sum with hexadecimal notation
	 * @throws NoSuchAlgorithmException
	 *             if the java version does not support the sha256 algorithm
	 * @throws IOException
	 *             if the file could not be operated upon
	 */
	public static String sha256sum(final File f)
			throws NoSuchAlgorithmException, IOException {
		if (!f.exists() || !f.isFile()) {
			throw new FileNotFoundException();
		}

		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		final FileInputStream fis = new FileInputStream(f);
		final byte[] dataBytes = new byte[4096];

		int nread = 0;

		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		fis.close();
		final StringBuffer sb = new StringBuffer(64);
		for (final byte b : md.digest()) {
			final int i = b + (b < 0 ? 256 : 0);
			final String acc = Integer.toHexString(i);
			if (acc.length() == 1) {
				sb.append('0');
			}
			sb.append(acc);
		}
		return sb.toString();
	}
}
