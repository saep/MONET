package com.github.monet.worker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.github.monet.common.CommonArgumentParser;
import com.github.monet.common.Config;

/**
 * Class with main method capable of starting the worker.
 */
public class WorkerMain {
	private final static Logger LOG = LogManager
			.getFormatterLogger(HostActivator.class);
	private HostActivator activator = null;
	private Felix felix = null;

	/**
	 * @see CommonArgumentParser
	 */
	private static class WorkerArgumentParser extends CommonArgumentParser {
		/**
		 * @see CommonArgumentParser#CommonArgumentParser(String, String)
		 */
		public WorkerArgumentParser() {
			super("MONetWorker",
					"Worker of the Monet Framework that runs experiments");
		}

	}

	public static void main(String[] args) {
		CommonArgumentParser argumentParser = new WorkerArgumentParser();
		ArgumentParser argparse = argumentParser.getArgumentParser();
		Config config = Config.getInstance();
		argparse.addArgument("--host")
				.setDefault(config.getHost()).type(String.class)
				.help("The address of the control server.");
		argparse.addArgument("--development")
				.type(Boolean.class)
				.setDefault(false)
				.help("Ignore the cache when downloading Bundles. This is "
						+ "useful if you want to test bundles on an existing "
						+ "controlserver.");

		/* Overwrite configuration parameters with command line parameters. */
		try {
			Namespace ns = argumentParser.parse(args);
			config.setHost(ns.getString("host"));
			config.setDevelopmentMode(ns.getBoolean("development"));
		} catch (ArgumentParserException e) {
			LOG.error(e.getLocalizedMessage());
			argparse.handleError(e);
			System.exit(CommonArgumentParser.RETURN_COMMANDLINE_OPTIONS_ERROR);
		} catch (IOException e) {
			LOG.error(e.getLocalizedMessage());
			System.exit(CommonArgumentParser.RETURN_CONTROLSERVER_IO_ERROR);
		}
		config.logConfig();
		final WorkerMain worker = new WorkerMain();
		/*
		 * Make sure that the Felix framework gets shut down when the java
		 * virtual machine is shut down
		 */
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				worker.shutdownApplication();
			}
		});
		worker.start();
	}

	public WorkerMain() {
		this.activator = new HostActivator();
	}

	public void start() {
		List<HostActivator> list = new ArrayList<HostActivator>();
		list.add(this.activator);
		// Create a configuration property map.
		Map<String, Object> config = new HashMap<String, Object>();
		config.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
		// add the system packages
		config.put(
				Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				"monet.common,monet.interfaces,monet.worker,monet.aggregators,org.apache.logging.log4j;version=\"2.0\",org.osgi.framework");
		// Control where OSGi stores its persistent data:
		config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
		try {
			// Now create an instance of the framework with
			// our configuration properties.
			this.felix = new Felix(config);
			// Now start Felix instance.
			this.felix.start();
		} catch (Exception ex) {
			LOG.error("Could not create framework: ", ex);
			this.shutdownApplication();
			System.exit(1);
		}
	}

	public Bundle[] getInstalledBundles() {
		return this.activator.getBundles();
	}

	/**
	 * Shut the Felix Framework and the Worker down.
	 */
	public synchronized void shutdownApplication() {
		try {
			this.felix.stop();
			this.felix.waitForStop(0);
		} catch (BundleException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// delete the felix-cache dir
		try {
			File felixCache = new File("felix-cache");
			FileUtils.deleteDirectory(felixCache);
		} catch (IOException e) {
			System.err.println("Failed to clean up the felix-cache.");
		}
	}
}
