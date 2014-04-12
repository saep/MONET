/**
 *
 */
package com.github.monet.common;

import java.io.File;
import java.io.IOException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * This abstract class contains the common command line arguments for the Worker
 * and ControlServer.
 */
public abstract class CommonArgumentParser {

	/**
	 * Return code for successful execution of MONET.
	 */
	public final static int RETURN_SUCCESS = 0;
	/**
	 * Code returned when MONET terminated with an unknown or unclassified
	 * error.
	 */
	public final static int RETURN_ERROR = 1;
	/**
	 * Code returned when MONET was passed the wrong command line options.
	 */
	public final static int RETURN_COMMANDLINE_OPTIONS_ERROR = 50;
	/**
	 * Code returned when MONET could not connect to MongoDB.
	 */
	public final static int RETURN_MONGO_CONNECTION_ERROR = 101;
	/**
	 * Code returned when MONET could not authenticate with MongoDB.
	 */
	public final static int RETURN_MONGO_AUTHENTICATION_ERROR = 102;
	/**
	 * Code returned when MONET could not resolve the MongoDB host.
	 */
	public final static int RETURN_MONGO_UNKNOWN_HOST_ERROR = 103;
	/**
	 * Code returned when MONETs ControlServer encountered an IOException.
	 */
	public final static int RETURN_CONTROLSERVER_IO_ERROR = 111;

	private final ArgumentParser argumentParser;

	/**
	 * This constructor initializes the underlying ArgumentParser with the
	 * common configuration options of the worker and control server.
	 * <p>
	 * To use this, you have to get the ArgumentParser object after initializing
	 * this object and you have to use this methods parse argument instead of
	 * the one providing by the argparse4j framework.
	 * </p>
	 * <p>
	 *
	 * <pre>
	 * private static class ContolServerArgumentParser extends CommonArgumentParser {
	 *
	 * 	public ContolServerArgumentParser() {
	 * 		super(&quot;MONetControlServer&quot;,
	 * 				&quot;descriptive text!&quot;);
	 * 	}
	 * }
	 * 	    [..]
	 * 		CommonArgumentParser argumentParser = new ContolServerArgumentParser();
	 * 		ArgumentParser argparse = argumentParser.getArgumentParser();
	 * 		argparse.addArgument("-u", "--upload").action(Arguments.storeTrue())
	 * 				.help("upload bundles from the cache directory");
	 * 		Namespace ns = null;
	 * 		try {
	 * 			ns = argumentParser.parse(args);
	 * 		} catch (ArgumentParserException e) {
	 * 		// something meaningful
	 * 		}
	 * 		[..]
	 * </pre>
	 *
	 * For a use of the Namespace object see the documentation for argparse4j.
	 *
	 * @param programName
	 *            the program name
	 * @param description
	 *            a (short) description of the program
	 * @see ArgumentParser
	 * @see Namespace
	 */
	public CommonArgumentParser(String programName, String description) {
		Config conf = Config.getInstance();
		argumentParser = ArgumentParsers.newArgumentParser(programName)
				.defaultHelp(true).description(description);
		argumentParser.addArgument("--dbport").setDefault(conf.getDBPort())
				.type(Integer.class).help("The port of the MongoDB server.");
		argumentParser.addArgument("--dbhost").setDefault(conf.getDBHost())
				.type(String.class).help("The address of the MongoDB server.");
		argumentParser.addArgument("--dbname").setDefault(conf.getDBName())
				.type(String.class).help("The name of the MongoDB database.");
		argumentParser.addArgument("--dbusername").type(String.class)
				.setDefault(conf.getDBUserName())
				.help("The user name of the MongoDB.");
		argumentParser.addArgument("--dbpassword").type(String.class)
				.setDefault(new String(conf.getDBPassword()))
				.help("The password of the MongoDB.");
		argumentParser.addArgument("--controlport").type(Integer.class)
				.setDefault(conf.getControlPort())
				.help("The port of the Monet Controlserver.");
		argumentParser
				.addArgument("-c", "--config-file")
				.setDefault(conf.getDefaultConfigurationFileName())
				.type(String.class)
				.help("This argument describes the path to the configuration "
						+ "file. If it is unset, the command line arguments "
						+ "take precedence over the default values.");
		argumentParser
				.addArgument("-d", "--documentation")
				.type(String.class)
				.setDefault(conf.getDocumentationrootDirectory())
				.help("This parameter describes the directory in which the "
						+ "markdown documenation is stored.");
		argumentParser
				.addArgument("--cache")
				.type(String.class)
				.setDefault(conf.getCache())
				.help("Define the directory in which non-temporary files are cached.");
	}

	/**
	 * @return the underling argument parser
	 *
	 * @see ArgumentParser
	 */
	public ArgumentParser getArgumentParser() {
		return this.argumentParser;
	}

	/**
	 * This function parses all entries which are present in the common
	 * configuration file.
	 * <p>
	 * This function exits the program with a pre-defined exit code if it
	 * encounters a DataBaseConnectionFailure.
	 *
	 * @param args
	 *            the command line arguments
	 * @return a namespace for the given command line arguments
	 * @throws ArgumentParserException
	 * @throws IOException
	 *
	 * @see DataBaseConnectionFailure
	 * @see Namespace
	 * @see Config
	 */
	public Namespace parse(String[] args) throws IOException,
			ArgumentParserException {
		Namespace ns = null;
		ns = argumentParser.parseArgs(args);
		final Config conf = Config.getInstance();
		final String configFile = ns.getString("config_file");
		if (configFile != null) {
			conf.setConfigFile(new File(configFile));
			Config.reload();
		}
		conf.setControlPort(ns.getInt("port"))
				.setDBHost(ns.getString("dbhost"))
				.setDBPort(ns.getInt("dbport"))
				.setDBName(ns.getString("dbname"))
				.setDBUsername(ns.getString("dbusername"))
				.setDBPassword(ns.getString("dbpassword"))
				.setControlPort(ns.getInt("controlport"))
				.setDocumentationRootDirectory(ns.getString("documentation"))
				.setCache(ns.getString("cache"));
		createCacheDirectoriesIfMissing();

		return ns;
	}

	public static void createCacheDirectoriesIfMissing() throws IOException {
		Config conf = Config.getInstance();
		createDirectory("cache", new File(conf.getCache()));
		createDirectory("bundle cache", new File(conf.getBundleCacheDir()));
		createDirectory("graph cache", new File(conf.getGraphCacheDir()));
	}

	/**
	 * Boilerplate directory creation nonsense.
	 *
	 * @param directoryType
	 *            just a name for error messages
	 * @param directory
	 *            the directory (if it is one)
	 * @throws IOException
	 *             if something is not right
	 */
	private static void createDirectory(String directoryType, File directory)
			throws IOException {
		if (directory.exists() && !directory.isDirectory()) {
			throw new IOException("The configured " + directoryType
					+ " directory is not a directory: "
					+ directory.getAbsolutePath());
		} else if (!directory.exists()) {
			if (!directory.mkdirs()) {
				throw new IOException("Could not create " + directoryType
						+ " directory in: " + directory.getAbsolutePath());
			}
		} else if (!directory.canWrite()) {
			throw new IOException("Cannot write to " + directoryType
					+ " directory: " + directory.getAbsolutePath());
		}
	}

}
