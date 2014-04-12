/**
 *
 */
package com.github.monet.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * This class represents the configuration options.
 * <p>
 * The configuration can be retrieved with the <tt>getInstance()</tt> method of
 * this class.
 * </p>
 * <p>
 * If you want to store or load the configuration settings, call the method
 * <tt>saveConfig()</tt> or <tt>reload()</tt> after settings the configuration
 * file with <tt>setConfigFile(File)</tt>.
 *
 */
public class Config {
	private static Logger LOG = LogManager.getLogger(Config.class);

	/**
	 * Singleton instance for the configuration file.
	 */
	private static Config instance = null;

	/**
	 * Singleton mongo client object which will create db connections as
	 * requested.
	 */
	private static MongoClient mongoClientInstance = null;

	/**
	 * The key-value map(ish) object storing all the configuration settings.
	 * These are read from the config.properties file.
	 */
	private Properties settings;

	/**
	 * Name of the expected configuration file.
	 */
	private File currentConfigFile;

	/**
	 * Private constructor called by the getInstance method to load the values
	 * from a "config.properties" file and override all default values with the
	 * items mentioned in that file.
	 */
	private Config() {
		settings = new Properties();
		this.setDefaultSettings();
		try {
			InputStream monetrc = Thread.currentThread()
					.getContextClassLoader().getResourceAsStream("monetrc");
			if (monetrc == null) {
				getClass().getClassLoader().getResourceAsStream("monetrc");
			}
			if (monetrc != null) {
				settings.load(monetrc);
			}
		} catch (IOException e) {
			LOG.error("Loading monetrc failed");
		}
	}

	/**
	 * Write the current configuration to the log.
	 * <p>
	 * This requires the log-level to be set to <tt>DEBUG</tt>.
	 */
	public void logConfig() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Loaded configuration file with the following settings.");
			LOG.debug("host:\t\t" + getHost());
			LOG.debug("ControlPort:\t" + getControlPort());
			LOG.debug("dbhost\t\t" + getDBHost());
			LOG.debug("dbport:\t\t" + getDBPort());
			LOG.debug("dbname:\t\t" + getDBName());
			LOG.debug("dbusername:\t" + getDBUserName());
			LOG.debug("dbpassword:\t" + new String(getDBPassword()));
			LOG.debug("cache:\t\t" + getCache());
			LOG.debug("development:\t" + Boolean.toString(getDevelopmentMode()));
			LOG.debug("documentation:\t" + getDocumentationrootDirectory());
		}
	}

	/**
	 * Reload the configuration file from disk (or from defaults if it is not
	 * present on disk).
	 */
	public static synchronized void reload() {
		if (instance == null) {
			instance = new Config();
		}
		if (instance.getConfigFile() != null) {
			if (instance.getConfigFile().exists()) {
				try {
					instance.loadConfigFromFile();
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		instance.setDefaultSettings();
	}

	/**
	 * Try to load the configuration settings from the file pointed to by the
	 * currentConfigFile field.
	 *
	 * @throws IOException
	 *             if the currentConfigFile field is null or the file cannot be
	 *             written to
	 */
	private synchronized void loadConfigFromFile() throws IOException {
		if (currentConfigFile == null) {
			throw new IOException(
					"The configuration file was not set and cannot be loaded.");
		}
		InputStream is = new FileInputStream(currentConfigFile);
		if (is != null) {
			settings.load(is);
		}
		for (Entry<Object, Object> e : settings.entrySet()) {
			Object key = e.getKey();
			Object value = e.getValue();
			if ((key instanceof String) && (value instanceof String)) {
				this.setValue((String) key, (String) value);
			} else {
				LOG.info("Key or value of unknown type. Will be ignored.");
				settings.remove(key);
			}
		}
		if (is != null) {
			is.close();
		}
	}

	/**
	 * Return the configuration file that the settings were read from or in
	 * which they should be saved.
	 * <p>
	 * This function may return null.
	 *
	 * @return the currently set configuration file
	 */
	public File getConfigFile() {
		return currentConfigFile;
	}

	/**
	 * Put all default values into the map.
	 */
	private void setDefaultSettings() {
		/* the port the control server listens on */
		setControlPort(33380);
		setHost("localhost");
		/* Mongo DB settings */
		setDBHost("localhost");
		setDBPort(27017);
		setDBUsername("");
		setDBPassword("");
		setDBName("monet");
		setDBTestName("monet-test");

		/* caches for bundle files, graphs etc. */
		setCache("monet_cache");
		setDevelopmentMode(false);

		/* documentation root directory */
		setDocumentationRootDirectory(getCache() + "/doc");

		File conf = instance == null ? new File("monetrc") : instance
				.getConfigFile();
		if ((conf != null) && conf.exists()) {
			setConfigFile(conf);
			try {
				loadConfigFromFile();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		} else {
			setConfigFile(conf);
		}
	}

	/**
	 * @return a newly created DB object for the testing database
	 * @throws UnknownHostException
	 */
	static synchronized DB createTestDB() throws UnknownHostException {
		/* Just to be on the safe side. */
		Config.getInstance();

		if (mongoClientInstance == null) {
			mongoClientInstance = new MongoClient(instance.getDBHost(),
					instance.getDBPort());
		}
		DB db = mongoClientInstance.getDB(instance.getTestDBName());
		if (instance.getDBUserName().length() > 0
				&& !db.authenticate(instance.getDBUserName(),
						instance.getDBPassword())) {
			throw new DataBaseConnectionFailure("Could not connect to databse.");
		}
		return db;
	}

	/**
	 * @return a db object created using the configuration options
	 */
	public static synchronized DB getDBInstance() {
		/* Just to be on the safe side. */
		Config.getInstance();

		if (mongoClientInstance == null) {
			try {
				mongoClientInstance = new MongoClient(instance.getDBHost(),
						instance.getDBPort());
			} catch (UnknownHostException e) {
				throw new DataBaseConnectionFailure(e);
			}
		}
		DB db = mongoClientInstance.getDB(instance.getDBName());
		if ((instance.getDBUserName().length() > 0)
				&& !db.authenticate(instance.getDBUserName(),
						instance.getDBPassword())) {
			throw new DataBaseConnectionFailure(
					"Could not authenticate with mongo db.");
		}
		return db;
	}

	/**
	 * Retrieve the configuration file instance or if none was generated yet,
	 * load the "config.properties" file. If the file is not present, default
	 * values are used.
	 *
	 * @return the configuration of the control server
	 */
	public static synchronized Config getInstance() {
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}

	/**
	 * Return the given value for the key.
	 * <p>
	 * May return null if the value was not set.
	 *
	 * @param key
	 *            the key whose value should be returned
	 * @return the value for the key or null if the value has never been set
	 */
	public String get(String key) {
		Object ret = settings.get(key);
		if (ret instanceof String) {
			return (String) ret;
		}
		return null;
	}

	/**
	 * @return the value for the "bundlecache" field
	 */
	public String getBundleCacheDir() {
		return getCache() + "/bundles";
	}

	public String getCache() {
		String ret = this.get("cache");
		return ret == null ? "" : ret;
	}

	/**
	 * @return the value for the "dbhost" field
	 */
	public String getDBHost() {
		String ret = this.get("dbhost");
		return ret == null ? "" : ret;
	}

	/**
	 * @return the value for the "dbname" field
	 */
	public String getDBName() {
		String ret = this.get("dbname");
		return ret == null ? "" : ret;
	}

	/**
	 * Conveniently converted to a character array which is required by the
	 * mongo db driver.
	 *
	 * @return the value for the "dbpassword" field
	 */
	public char[] getDBPassword() {
		String ret = this.get("dbpassword");
		return ret == null ? new char[0] : ret.toCharArray();
	}

	/**
	 * @return the value for the "dbport" field conveniently converted to an
	 *         integer
	 */
	public int getDBPort() {
		return Integer.parseInt(this.get("dbport"));
	}

	/**
	 * @return the value for the "dbusername" field
	 */
	public String getDBUserName() {
		String ret = this.get("dbusername");
		return ret == null ? "" : ret;
	}

	/**
	 * @return the default configuration file name
	 */
	public String getDefaultConfigurationFileName() {
		return "monetrc";
	}

	/**
	 * @return whether the worker uses the cache for bundles or downloads the
	 *         bundles every time he needs them
	 */
	public Boolean getDevelopmentMode() {
		String ret = get("development");
		return ret == null ? false : Boolean.parseBoolean(ret);
	}

	/**
	 * @return the directory for the documentation
	 */
	public String getDocumentationrootDirectory() {
		String ret = (String) settings.get("documentation");
		return ret == null ? "." : ret;
	}

	/**
	 * @return the value for the "graphcache" field
	 */
	public String getGraphCacheDir() {
		return getCache() + "/graphs";
	}

	/**
	 * @return the value for the "host" field
	 */
	public String getHost() {
		String ret = this.get("host");
		return ret == null ? "" : ret;
	}

	/**
	 * @return the value for the "logfile" field
	 */
	public String getLogFile() {
		return getCache() + "/log";
	}

	/**
	 * @return the value for the "controlport" field
	 */
	public int getControlPort() {
		return Integer.parseInt(get("controlport"));
	}

	/**
	 * @return the value for the "testdbname" field
	 */
	public String getTestDBName() {
		String ret = this.get("testdbname");
		return ret == null ? "" : ret;
	}

	public synchronized Config setCache(String cache) {
		return setValue("cache", cache);
	}

	/**
	 * Set the file to which the configuration can be saved or from which the
	 * configuration can be read.
	 *
	 * @param configFile
	 *            the configuration file
	 */
	public synchronized Config setConfigFile(File configFile) {
		if ((configFile != null) && configFile.exists()
				&& !configFile.isDirectory() && configFile.canRead()) {
			currentConfigFile = configFile;
		}
		return instance;
	}

	/**
	 * Set the hostname for the database server.
	 *
	 * @param dbHost
	 *            the database server's host name
	 */
	public synchronized Config setDBHost(String dbHost) {
		return setValue("dbhost", dbHost);
	}

	/**
	 * Set the collection's name of the database.
	 *
	 * @param dbName
	 *            the name of the database collection
	 */
	public synchronized Config setDBName(String dbName) {
		return setValue("dbname", dbName);
	}

	/**
	 * Set the password for the database connection.
	 *
	 * @param dbPassword
	 *            the password for the database
	 */
	public synchronized Config setDBPassword(String dbPassword) {
		return setValue("dbpassword", dbPassword);
	}

	/**
	 * Set the port for the database connection.
	 *
	 * @param dbPort
	 *            the database server's port
	 */
	public synchronized Config setDBPort(Integer dbPort) {
		return setValue("dbport", dbPort.toString());
	}

	/**
	 * Set the collection name for the test database collection.
	 *
	 * @param name
	 *            set the name of the test database collection
	 */
	public synchronized Config setDBTestName(String name) {
		return setValue("testdbname", name);
	}

	/**
	 * Set the username for the database connection.
	 *
	 * @param dbUser
	 *            the username of the database's user
	 */
	public synchronized Config setDBUsername(String dbUser) {
		return setValue("dbusername", dbUser);
	}

	/**
	 * Option that forces the worker to download all bundles every time they are
	 * requested.
	 *
	 * @param b
	 *            whether to use the development mode
	 * @return the updated configuration object
	 */
	public Config setDevelopmentMode(boolean b) {
		return setValue("development", Boolean.toString(b));
	}

	/**
	 * @param directory
	 *            the directory with the documentation
	 */
	public synchronized Config setDocumentationRootDirectory(String directory) {
		return setValue("documentation", directory);
	}

	/**
	 * Set the hostname for the database connection.
	 *
	 * @param host
	 *            the hostname of the controlserver or worker
	 */
	public synchronized Config setHost(String host) {
		return setValue("host", host);
	}

	/**
	 * Set the port of the controlserver or the worker (depending on context).
	 *
	 * @param port
	 *            the control server's port
	 */
	public synchronized Config setControlPort(Integer port) {
		if (port != null) {
			setValue("controlport", port.toString());
		}
		return instance;
	}

	/**
	 * Set a single value in the config file.
	 * <p>
	 * This change will only be saved if the configuration file is saved later,
	 * i. e. the <tt>saveConfig()</tt> method is called. Otherwise it is present
	 * until the program closes or the key is overwritten (again).
	 * </p>
	 *
	 * @param key
	 *            the key to set
	 * @param value
	 *            the (new) value for the key
	 */
	public synchronized Config setValue(String key, String value) {
		settings.put(key, value);
		return instance;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("File: ")
				.append(currentConfigFile == null ? "unset" : currentConfigFile
						.getAbsolutePath()).append("\n");
		for (Entry<Object, Object> e : settings.entrySet()) {
			sb.append(e.getKey().toString()).append(" : ")
					.append(e.getValue().toString()).append("\n");
		}
		return sb.toString();
	}
}
