package com.github.monet.worker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.BundleValidationException;
import com.github.monet.common.Checksum;
import com.github.monet.common.Config;
import com.github.monet.common.DBCollections;
import com.github.monet.common.DependencyManager;
import com.github.monet.common.DependencyManager.ResolveDependenciesException;
import com.github.monet.interfaces.Algorithm;
import com.github.monet.interfaces.GraphParser;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

/**
 * The ServiceDirectory stores Algorithms, GraphParsers and other services for
 * retrieval.
 *
 * <p>
 * All services have a descriptor of the form:<code>bundle#version</code> Where
 * bundle is the name of the OSGi bundle and version the bundle's version.
 * </p>
 *
 * @author Sebastian Witte, Max Günther
 *
 */
public class ServiceDirectory {
	private final static Logger log = LogManager
			.getFormatterLogger(ServiceDirectory.class);
	private BundleContext context;
	private GridFS bundle_files;
	private ServiceUpdates serviceUpdates;
	private Map<BundleDescriptor, File> cachedBundles;
	private Map<BundleDescriptor, Bundle> activeBundles;
	private Map<BundleDescriptor, Bundle> installedBundles;
	private Set<ServiceReference<?>> activeServices;

	/**
	 * Create a service directory object for the given bundle context.
	 *
	 * @param bundleContext
	 *            the bundle context
	 * @throws ServiceDirectoryFailure
	 *             if the bundle cache directory could not be created or written
	 *             to
	 * @see BundleContext
	 */
	ServiceDirectory(BundleContext bundleContext)
			throws ServiceDirectoryFailure {
		// create essential attributes
		this.context = bundleContext;
		this.bundle_files = new GridFS(Config.getDBInstance(),
				DBCollections.BUNDLE_FILES);
		this.serviceUpdates = new ServiceUpdates();
		this.context.addServiceListener(this.serviceUpdates);
		this.cachedBundles = new HashMap<>();
		this.activeBundles = new HashMap<>();
		this.installedBundles = new HashMap<>();
		this.activeServices = new HashSet<>();

		File bundleDir = new File(Config.getInstance().getBundleCacheDir());
		if (bundleDir != null && !bundleDir.exists()) {
			boolean success = bundleDir.mkdir();
			if (!success) {
				throw new ServiceDirectoryFailure(String.format(
						"bundle directory \"%s\" couldn't be created",
						bundleDir.toString()));
			}
		}

		// find all bundles and put them into the cached set
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		};
		for (String bundleFileStr : bundleDir.list(filter)) {
			File bundleFile = new File(bundleDir, bundleFileStr);
			String bundlePath = bundleFile.getAbsolutePath();
			try {
				BundleDescriptor descriptor = BundleDescriptor
						.fromJar(bundlePath);
				this.cachedBundles.put(descriptor, bundleFile);
				log.debug("found in cache: %s", descriptor);
			} catch (BundleValidationException e) {
				log.error(e);
			}
		}

		// validate the cached bundles
		this.validateCachedBundles();
	}

	/**
	 * Returns an Algorithm given its descriptor string.
	 *
	 * @param descriptor
	 *            the descriptor of the algorithm to return
	 * @return the instantiated algorithm, ready for execution
	 */
	public Algorithm getAlgorithm(String descriptor)
			throws ServiceNotFoundException, BundleException {
		return this.getService(descriptor, Algorithm.class);
	}

	/**
	 * Returns a GraphParser given its descriptor string.
	 *
	 * @param descriptor
	 *            the descriptor of the graph parser to return
	 * @return the instantiated graph parser, ready for parsing
	 */
	public GraphParser getGraphParser(String descriptor)
			throws ServiceNotFoundException, BundleException {
		return this.getService(descriptor, GraphParser.class);
	}

	/**
	 * Return a service given the descriptor of its bundle and the class of the
	 * service.
	 *
	 * @param descriptor
	 *            the descriptor of the bundle containing the service
	 * @param clazz
	 *            the class/interface of the service
	 * @return the service
	 * @throws ServiceNotFoundException
	 *             raised if the service could not be found locally nor be
	 *             fetched and install from the controlserver
	 */
	public <S> S getService(String descriptor, Class<S> clazz)
			throws ServiceNotFoundException, BundleException {
		if (!BundleDescriptor.isValid(descriptor)) {
			throw new ServiceNotFoundException(String.format(
					"The supplied bundle descriptor has an invalid format: %s",
					descriptor));
		}
		BundleDescriptor desc;
		try {
			desc = new BundleDescriptor(descriptor);
		} catch (BundleValidationException e) {
			throw new BundleException(e.getLocalizedMessage(), e);
		}
		log.debug("getting bundle %s", descriptor);
		// ensure the service is active and running
		if (this.activeBundles.get(desc) != null) {
			log.debug("bundle %s already active", descriptor);
		} else if (this.getInstalledBundleDescriptor(descriptor) != null) {
			desc = this.getInstalledBundleDescriptor(descriptor);
			log.debug("bundle %s already installed, starting...", descriptor);
			this.startBundle(desc);
		} else if (!Config.getInstance().getDevelopmentMode()
				&& this.cachedBundles.get(desc) != null
				&& !downloadBundleAndCompareChecksum(desc)) {
			desc.setFile(this.cachedBundles.get(desc));
			log.debug("bundle %s found in cache, starting...", descriptor);
			// bundle is cached
			this.startBundle(desc);
		} else {
			log.debug("looking bundle %s up on controlserver", descriptor);
			// bundle is looked up on the controlserver
			this.retrieveBundleFromControlServer(desc);
			this.startBundle(desc);
		}
		// the bundle is now active and the service can be found
		return this.activateService(desc, clazz);
	}

	/**
	 * Activate a service given a descriptor.
	 *
	 * @param descriptor
	 *            the descriptor of the service/bundle
	 * @param clazz
	 *            the class of the service
	 * @return the service, ready to use
	 * @throws ServiceNotFoundException
	 *             if the service couldn't be found
	 */
	private <S> S activateService(BundleDescriptor descriptor, Class<S> clazz)
			throws ServiceNotFoundException {
		Bundle bundle = this.activeBundles.get(descriptor);
		if (bundle.getRegisteredServices() == null) {
			String err = String.format("the service %s couldn't be found in"
					+ " bundle %s", clazz.getName(), bundle);
			throw new ServiceNotFoundException(err);
		}
		for (ServiceReference<?> ref : bundle.getRegisteredServices()) {
			String[] classes = (String[]) ref
					.getProperty(Constants.OBJECTCLASS);
			for (String objectClass : classes) {
				if (objectClass.equals(clazz.getName())) {
					this.activeServices.add(ref);
					@SuppressWarnings("unchecked")
					S service = (S) this.context.getService(ref);
					return service;
				}
			}
		}

		String err = String.format("the service %s from bundle \"%s\" was"
				+ " supposed to be active but couldn't be found",
				clazz.getName(), descriptor);
		throw new ServiceNotFoundException(err);
	}

	/**
	 * Returns the BundleDescriptor for a descriptor string of an installed
	 * bundle.
	 *
	 * @param descriptor
	 *            the descriptor of the installed bundle as a string
	 * @return the BundleDescriptor of the installed bundle, or null
	 */
	private BundleDescriptor getInstalledBundleDescriptor(String descriptor) {
		for (BundleDescriptor d : this.installedBundles.keySet()) {
			if (d.getDescriptor().equals(descriptor)) {
				return d;
			}
		}
		return null;
	}

	/**
	 * Look a bundle up on the controlserver and transfer it into the cache.
	 *
	 * @param descriptor
	 *            the descriptor of the bundle to load
	 * @throws ServiceNotFoundException
	 *             if there is no such bundle on the controlserver
	 */
	private void retrieveBundleFromControlServer(BundleDescriptor descriptor)
			throws ServiceNotFoundException {
		// get the name of the bundle on the controlserver
		// find the file on the controlserver
		GridFSDBFile dbfile = this.bundle_files.findOne(descriptor
				.getDescriptor());
		// check the existence
		if (dbfile == null) {
			throw new ServiceNotFoundException(String.format(
					"the bundle \"%s\" could not be found", descriptor));
		}
		// create the cached bundle file
		File localBundleFile = new File(Config.getInstance()
				.getBundleCacheDir(), descriptor.getCleanJarName());
		// write the bundle to the local cache
		try {
			dbfile.writeTo(localBundleFile);
			this.cachedBundles.put(descriptor, localBundleFile);
			descriptor.setFile(localBundleFile);
		} catch (IOException e) {
			throw new ServiceNotFoundException(
					"IOException while retrieving bundle from server", e);
		}
	}

	/**
	 * Start a bundle from the local cache.
	 *
	 * @param descriptor
	 *            the descriptor of the bundle to start
	 * @throws BundleException
	 *             raised if OSGi doesn't like the bundle
	 * @throws ServiceNotFoundException
	 *             raised if the bundle could not be found in the cache
	 */
	private void startBundle(BundleDescriptor descriptor)
			throws BundleException, ServiceNotFoundException {
		// get all dependent bundles
		DependencyManager dm = DependencyManager.getInstance();
		Collection<BundleDescriptor> dependencies;
		try {
			dependencies = dm.resolveDependencies(descriptor);
		} catch (ResolveDependenciesException e) {
			throw new ServiceNotFoundException(
					"Could not resolve dependencies", e);
		}
		log.debug(dependencies);
		// install all dependencies if they are not yet installed
		for (BundleDescriptor d : dependencies) {
			if (this.installedBundles.containsKey(d)) {
				continue;
			} else if (this.cachedBundles.get(d) == null) {
				// fetch dependency from controlserver
				this.retrieveBundleFromControlServer(d);
			}
			// the bundle should now definitely be in the cache
			d.setFile(this.cachedBundles.get(d));

			File bundleLocation = d.getFile();
			if (!bundleLocation.exists()) {
				String err = String
						.format("the bundle \"%s\" was supposed to be"
								+ " cached but could not be found in the cache",
								descriptor);
				throw new ServiceNotFoundException(err);
			}
			String uri = bundleLocation.toURI().toString();
			Bundle bundle = this.context.installBundle(uri);
			log.debug("installed bundle %s", d);
			this.installedBundles.put(d, bundle);
		}

		// install the actual bundle supplied via parameter
		Bundle startBundle = null;
		if (!this.installedBundles.containsKey(descriptor)) {
			log.debug("installing %s", descriptor);
			String uri = descriptor.getFile().toURI().toString();
			startBundle = this.context.installBundle(uri);
			this.installedBundles.put(descriptor, startBundle);
		}

		// start the actual bundle supplied via parameter
		if (!this.activeBundles.containsKey(descriptor)) {
			log.debug("starting bundle %s", descriptor);
			startBundle = this.installedBundles.get(descriptor);
			startBundle.start();
			this.activeBundles.put(descriptor, startBundle);
		}
	}

	/**
	 * Stops all active bundles. This should be called after every experiment to
	 * ensure a clear working environment. After this has been called all
	 * bundles are uninstalled.
	 */
	void stopAllBundles() {
		log.debug("all bundles are being stopped");
		for (Bundle b : this.activeBundles.values()) {
			if (b == null) {
				continue;
			}
			try {
				if (b.getRegisteredServices() != null) {
					for (ServiceReference<?> s : b.getRegisteredServices()) {
						// the success of the removal doesn't matter; it only
						// matters that the service isn't in the active services
						// anymore
						this.activeServices.remove(s);
					}
				}
				b.stop();
			} catch (BundleException e) {
				log.warn(e);
			}
		}
		this.activeBundles.clear();
		if (this.activeServices.size() > 0) {
			log.warn("after a ServiceDirectory was reset some"
					+ " some services weren't cleaned up: "
					+ this.activeServices.toString());
			this.activeServices.clear();
		} else {
			log.debug("all bundles were stopped");
		}
		for (Bundle b : this.installedBundles.values()) {
			try {
				b.uninstall();
			} catch (BundleException e) {
				log.warn("tried to uninstall bundle %s but couldn't", b);
			}
		}
		this.installedBundles.clear();
	}

	/**
	 * Validate that all bundles in the cache are up-to-date using the
	 * controlserver. Delete any bundles that aren't up-to-date.
	 */
	void validateCachedBundles() {
		log.info("validating cached bundles...");
		Collection<BundleDescriptor> invalid = new LinkedList<>();
		for (BundleDescriptor bd : this.cachedBundles.keySet()) {
			if (!downloadBundleAndCompareChecksum(bd)) {
				invalid.add(bd);
			}
		}
		for (BundleDescriptor bd : invalid) {
			this.cachedBundles.remove(bd);
			bd.getFile().delete();
			log.debug("removed invalid bundle %s from cache", bd);
		}
		log.info("cache validated");
	}

	/**
	 * @param bundleDescriptor
	 *            the bundle descriptor indicating which bundle to download
	 * @return true if the checksums match
	 */
	private boolean downloadBundleAndCompareChecksum(BundleDescriptor bundleDescriptor) {
		if (bundleDescriptor.getFile() == null) {
			return false;
		}

		GridFSDBFile bundle = this.bundle_files.findOne(bundleDescriptor.getDescriptor());
		if (bundle != null) {
			Object o = bundle.get("hash");
			try {
				String bundleDescriptChecksum = Checksum.sha256sum(bundleDescriptor.getFile());
				if (o instanceof String) {
					String hashInDatabase = (String) o;
					return hashInDatabase.equalsIgnoreCase(bundleDescriptChecksum);
				}
			} catch (NoSuchAlgorithmException | IOException e) {
				log.error(e);
			}
		}
		return false;
	}

	/**
	 * Convenience method for correctly registering an algorithm with OSGi.
	 *
	 * @param context
	 *            the BundleContext of the bundle implementing the algorithm
	 * @param newAlgorithm
	 *            the new Algorithm
	 */
	public static void registerAlgorithm(BundleContext context,
			Algorithm newAlgorithm) {
		log.debug("registering algorithm: %s", newAlgorithm.getClass()
				.toString());
		Hashtable<String, String> props = new Hashtable<String, String>();
		context.registerService(Algorithm.class.getName(), newAlgorithm, props);
	}

	/**
	 * Convenience method for correctly registering a graph parser with OSGi.
	 *
	 * @param context
	 *            the BundleContext of the bundle implementing the new graph
	 *            parser
	 * @param graphParser
	 *            the new GraphParser
	 */
	public static void registerGraphParser(BundleContext context,
			GraphParser graphParser) {
		Hashtable<String, String> props = new Hashtable<String, String>();
		context.registerService(GraphParser.class.getName(), graphParser, props);
	}

	/**
	 * Handles all the service updates.
	 *
	 * @author Max Günther
	 *
	 */
	private class ServiceUpdates implements ServiceListener {
		/**
		 * Called when any services changes, even if it's a service we don't
		 * care about.
		 */
		@Override
		public void serviceChanged(ServiceEvent event) {
			if (event.getType() == ServiceEvent.REGISTERED) {
			} else if (event.getType() == ServiceEvent.UNREGISTERING) {
				if (ServiceDirectory.this.activeServices.contains(event
						.getServiceReference())) {
					// TODO for the moment just log this event, depending on
					// what
					// actually happens when a service is unregistered that is
					// being used in the current experiment. If anything serious
					// happens the current experiment may need to be stopped and
					// put into a failed state.
					log.error("a service that is being used in the "
							+ "current experiment was unregistered");
				}
			}
		}
	}

}
