/**
 *
 */
package com.github.monet.common;

import java.io.InputStream;

import com.github.monet.common.Config;
import com.github.monet.common.FileUtils;

/**
 *
 */
public class TestConfig {
	/**
	 * Find a file with the given name and set it as the config.properties file.
	 *
	 * @param fileName
	 *            the name of the file
	 * @return a config file from the file (or the default settings)
	 */
	public static Config loadConfig(String fileName) {
		Config conf = Config.getInstance();
		InputStream is = ClassLoader.getSystemResourceAsStream(fileName);
		if (is != null) {
			conf.setConfigFile(FileUtils.createTempFileFromStream("config",
					".properties", is));
			Config.reload();
		}
		return conf;
	}
}
