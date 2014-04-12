package com.github.monet.worker;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.github.monet.common.Config;
import com.mongodb.DB;

/**
 * Activates the worker and creates all objects that exist over the enitre
 * runtime of the worker.
 *
 * @author Max Günther
 *
 */
public class HostActivator implements BundleActivator {
	private final static Logger log = LogManager
			.getFormatterLogger(HostActivator.class);
	private BundleContext bundleContext;
	private Experimentor experimentor;
	private Communicator communicator;
	private ServiceDirectory serviceDirectory;
	private DB db;

	public HostActivator() {
		db = Config.getDBInstance();
		bundleContext = null;
		experimentor = null;
		communicator = null;
	}

	/**
	 * This method is called when the worker is created.
	 *
	 * Everything needs to be initialized and started from here.
	 *
	 * @throws ServiceDirectoryFailure
	 * @throws DatabaseFailure
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		// hold the bundle context
		this.bundleContext = bundleContext;

		// create important and central objects
		serviceDirectory = new ServiceDirectory(bundleContext);
		experimentor = new Experimentor(serviceDirectory, db);

		try {
			communicator = new Communicator(experimentor);
			experimentor.addObserver(communicator);
			communicator.start();
		} catch (IOException ex) {
			log.error(ex);
			String errmsg = String.format("failed to establish a connection "
					+ "to %s on port %d\n", Config.getInstance().getHost(),
					Config.getInstance().getControlPort());
			throw new IOException(errmsg, ex);
		}
	}

	/**
	 * Called when the worker is being terminated.
	 */
	@Override
	public void stop(BundleContext context) {
		log.info("Worker stopped.");
	}

	public Bundle[] getBundles() {
		if (this.bundleContext != null) {
			return this.bundleContext.getBundles();
		}
		return null;
	}

}
