package com.github.monet.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.BasicBSONList;

import com.github.monet.common.BundleDescriptor.Kind;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;

/**
 * The dependency manager provides convenience functions to query the data base
 * for bundles as well as a mechanism to resolve dependencies.
 */
public class DependencyManager {
	/**
	 * This is the omnipotent data base object on which all data base accesses
	 * are done
	 */
	private final DB db;
	private final GridFS bundleFiles;
	private static Logger LOG = LogManager.getLogger(DependencyManager.class);

	/**
	 * The dependency manager instance.
	 */
	private static DependencyManager instance = null;

	/**
	 * Create a dependency manager for the given data base.
	 *
	 * @param db
	 *            the data base
	 */
	private DependencyManager(DB db) {
		this.db = db;
		this.bundleFiles = new GridFS(this.db, DBCollections.BUNDLE_FILES);
	}

	/**
	 * @return the dependency manager instance
	 */
	public static synchronized DependencyManager getInstance() {
		if (instance == null) {
			instance = new DependencyManager(Config.getDBInstance());
		}
		return instance;
	}

	/**
	 * Resolve the dependencies for the given bundles automatically.
	 *
	 * @param descriptors
	 *            the bundle descriptors whose dependencies should be resolved
	 * @return a set of bundles that fulfill all dependencies
	 */
	public Collection<BundleDescriptor> resolveDependencies(
			BundleDescriptor... descriptors)
			throws ResolveDependenciesException {
		List<BundleDescriptor> neededBundles = new LinkedList<>();
		Set<VersionedPackage> toResolve = new TreeSet<>();
		Set<BundleDescriptor> resolved = new TreeSet<>();
		for (BundleDescriptor bd : descriptors) {
			try {
				toResolve.addAll(this.getDependencies(bd));
			} catch (BundleNotFoundException | BundleValidationException e) {
				throw new ResolveDependenciesException(e.getMessage(), e);
			}
			resolved.addAll(this.exportedBy(bd));
		}
		if (!toResolve.isEmpty()) {
			try {
				resolve(neededBundles, toResolve, resolved);
				return this.orderDependencies(neededBundles);
			} catch (BundleNotFoundException | BundleValidationException e) {
				throw new ResolveDependenciesException(e.getMessage(), e);
			}
		} else {
			return new LinkedList<>();
		}
	}

	/**
	 * Get a set of all bundles exporting the given package and fulfill the
	 * version string.
	 *
	 * @param packageName
	 *            the package name which a bundle should export
	 */
	public Set<BundleDescriptor> getProviders(final String packageName) {
		try {
			final DBCollection coll = this.db
					.getCollection(DBCollections.EXPORTERTABLE);
			Set<BundleDescriptor> provs = new TreeSet<>();
			DBObject findOne = coll.findOne(packageName);
			if (findOne == null) {
				return provs;
			}
			DBObject bundles = (DBObject) findOne
					.get(DBCollections.BUNDLE_FILES);
			Map<?, ?> map = bundles.toMap();
			for (Entry<?, ?> e : map.entrySet()) {
				Collection<?> versions = (Collection<?>) e.getValue();
				String name = (String) e.getKey();
				for (Object v : (Collection<?>) versions) {
					provs.add(new BundleDescriptor(name, (String) v));
				}
			}
			return provs;
		} catch (NullPointerException | ClassCastException
				| BundleValidationException e) {
			throw new RuntimeException("Unexpected data base entry for '"
					+ packageName + "'.", e);
		}
	}

	/**
	 * Retrieve the package names that the given bundle exports.
	 *
	 * @param descriptor
	 *            the bundle descriptor
	 * @return a collection of exported package names
	 */
	public Collection<BundleDescriptor> exportedBy(BundleDescriptor descriptor) {
		GridFSDBFile entry = this.bundleFiles.findOne(descriptor.getDescriptor());
		if (entry == null) {
			LOG.error(String
					.format("The given bundle %s is not (properly) existent in the data base",
							descriptor.getDescriptor()));
			return new LinkedList<>();
		}

		BasicDBList o = (BasicDBList) entry.get("exports");
		Collection<BundleDescriptor> retVal = new LinkedList<>();
		for (Object acc : o) {
			BasicDBObject tmp = (BasicDBObject) acc;
			String name = (String) tmp.get("name");
			String version = (String) tmp.get("version");
			try {
				retVal.add(new BundleDescriptor(name, version));
			} catch (BundleValidationException e) {
				e.printStackTrace();
				// This really should not happen!
				throw new RuntimeException("Inconsistent data base entries!");
			}
		}
		return retVal;
	}

	/**
	 * Returns all bundles that are registered in the data base.
	 * <p>
	 *
	 * @return all bundles
	 *
	 */
	public Collection<BundleDescriptor> getAllBundles() {
		Collection<BundleDescriptor> bundles = new ArrayList<>();
		DBCursor it = this.bundleFiles.getFileList();
		while (it.hasNext()) {
			if (it.next() instanceof GridFSDBFile) {
				GridFSFile gridFSFile = (GridFSFile) it.curr();
				if (gridFSFile.getFilename() != null) {
					try {
						BundleDescriptor tmp = new BundleDescriptor(
								gridFSFile.getFilename());
						final File output = getFile(new BundleDescriptor(
								tmp.getDescriptor()));
						BundleDescriptor bd = BundleDescriptor.fromFile(output);
						bundles.add(bd);
					} catch (BundleValidationException e) {
						e.printStackTrace();
						throw new RuntimeException(
								"The bundle's in the data base should have the "
										+ "correct meta data information.");
					} catch (FileNotFoundException e) {
						throw new RuntimeException(e.getMessage(), e);
					} catch (IOException e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			}
		}
		return bundles;
	}

	/**
	 * This function gets all bundles from the database and allows only those
	 * whose type equals the given type.
	 *
	 * @param type
	 *            the type to allow
	 * @return a collection of bundle descriptors
	 */
	public Collection<BundleDescriptor> getAllBundlesOfType(
			BundleDescriptor.Kind type) {
		Collection<BundleDescriptor> all = getAllBundles();
		Collection<BundleDescriptor> result = new LinkedList<>();
		for (BundleDescriptor a : all) {
			if (a.kind() == Kind.GENERIC) {
				/*
				 * This is even necessary for the GENERIC type as the bundle
				 * descriptor might become non-GENERIC.
				 */
				BundleDescriptor bd = a.createSpecific(this);
				if (bd.kind() == type) {
					result.add(bd);
				}
			}
			if (a.kind() == type) {
				result.add(a);
			}
		}
		return result;
	}

	/**
	 * Retrieve a bundle file from the data base for the given descriptor.
	 *
	 * @param descriptor
	 *            bundle descriptor
	 * @return the bundle jar or null if none exists
	 * @throws IOException
	 *             if some IO error occurs
	 *
	 */
	public File getFile(final BundleDescriptor descriptor) throws IOException {
		final GridFSDBFile dbf = this.bundleFiles.findOne(descriptor
				.getDescriptor());
		if (dbf == null) {
			return null;
		}
		final InputStream fis = dbf.getInputStream();
		File output = new File(Config.getInstance().getBundleCacheDir(),
				descriptor.getCleanJarName());
		if (output.exists()) {
			output.delete();
		}
		Object o;
		final FileOutputStream fos = new FileOutputStream(output);
		int n;
		final byte[] bytes = new byte[4096];
		while ((n = fis.read(bytes)) > 0) {
			fos.write(bytes, 0, n);
		}
		fos.close();
		try {
			o = dbf.get("hash");
			if (o instanceof String) {
				((String) o).equalsIgnoreCase(Checksum.sha256sum(output));
			} else {
				throw new IOException(
						"Bundle file in the data base has no hash field.");
			}
		} catch (NoSuchAlgorithmException e) {
			/*
			 * This really shouldn't happen and would only bloat the usage of
			 * this function
			 */
			throw new IOException("Checksum algorithm not found!", e);
		}
		return output;
	}

	/**
	 * Returns the import-package names for the given bundle descriptor, but
	 * does not resolve any further (recursive) dependencies.
	 *
	 * @param descriptor
	 *            the bundle descriptor
	 * @return a set of dependencies for the given descriptor or null if no
	 *         entry is available
	 * @throws BundleNotFoundException
	 *             if the bundle was not found in the data base
	 * @throws BundleValidationException
	 *             if the objects stored in the database are invalid
	 */
	private Collection<VersionedPackage> getDependencies(
			final BundleDescriptor descriptor) throws BundleNotFoundException,
			BundleValidationException {
		DBObject bundle = this.bundleFiles.findOne(descriptor.getDescriptor());
		LinkedList<VersionedPackage> deps = new LinkedList<>();
		if (bundle == null) {
			throw new BundleNotFoundException(String.format(
					"The bundle %s was not present in the data base",
					descriptor.getDescriptor()));
		}
		BasicBSONList o = (BasicBSONList) bundle.get("imports");
		deps = new LinkedList<>();
		for (Object dep : o) {
			BasicDBObject tmp = (BasicDBObject) dep;
			String version = (String) tmp.get("version");
			String name = (String) tmp.get("name");
			deps.add(new VersionedPackage(name, version));
		}
		return deps;
	}

	/**
	 * Determines the order in which the bundles have to be loaded.
	 * <p>
	 * This function creates a new List that contains bundle descriptors in a
	 * specific order. If the bundles are loaded in this order, no dependency
	 * conflicts should occur.
	 *
	 * @param neededBundles
	 *            the dependencies to order
	 * @return a new list of bundle descriptors
	 * @throws ResolveDependenciesException
	 *             if there is no bundle without dependencies
	 * @throws BundleNotFoundException
	 *             if the bundle file could not be found in the data base
	 * @throws BundleValidationException
	 *             if a bundle in the data base was stored wrong
	 */
	private List<BundleDescriptor> orderDependencies(
			List<BundleDescriptor> neededBundles)
			throws ResolveDependenciesException, BundleNotFoundException,
			BundleValidationException {
		Collection<VersionedPackage> acc;
		Set<BundleDescriptor> resolved = new TreeSet<>();
		List<BundleDescriptor> orderedBundles = new ArrayList<>(
				neededBundles.size());
		boolean progress;
		do {
			progress = false;
			for (BundleDescriptor b : neededBundles) {
				acc = this.getDependencies(b);
				acc.removeAll(resolved);
				if (acc.isEmpty()) {
					orderedBundles.add(b);
					resolved.addAll(this.exportedBy(b));
					progress = true;
					neededBundles.remove(b);
					break;
				}
			}
		} while (progress);
		if (neededBundles.isEmpty()) {
			return orderedBundles;
		} else {
			throw new ResolveDependenciesException(
					"Dependency chain contains circles!");
		}
	}

	/**
	 * Private worker method for
	 * {@link DependencyManager#resolveDependencies(BundleDescriptor)} which
	 * utilizes a set accumulator that holds unresolved dependencies.
	 *
	 * @param toResolve
	 *            set containing the not yet resolved dependencies
	 * @param resolved
	 *            set containing the already resolved dependencies
	 * @return a set of bundles that would resolve the dependencies
	 * @throws ResolveDependenciesException
	 *             if a dependency could not be resolved
	 * @throws BundleNotFoundException
	 *             if a requested bundle does not exist
	 * @throws BundleValidationException
	 *
	 */
	private void resolve(final List<BundleDescriptor> ret,
			Set<VersionedPackage> toResolve, Set<BundleDescriptor> resolved)
			throws ResolveDependenciesException, BundleNotFoundException,
			BundleValidationException {
		boolean abort = true; // fall back condition
		boolean modified = false;
		for (VersionedPackage a : toResolve) {
			if (!resolved.contains(a)) {
				Set<BundleDescriptor> providers = this
						.getProviders(a.getName());
				if ((providers == null) || (providers.size() == 0)) {
					throw new ResolveDependenciesException(String.format(
							"No providers for %s available", a));
				}
				/* The loop only exists to extract the first element. */
				for (BundleDescriptor b : providers) {
					if (!ret.contains(b)) {
						ret.add(b);
					}
					for (BundleDescriptor iface : this.exportedBy(b)) {
						modified |= toResolve.remove(iface);
						resolved.add(iface);
						abort = false; // something changed
					}
					for (VersionedPackage iface : this.getDependencies(b)) {
						if (!resolved.contains(iface)) {
							toResolve.add(iface);
						}
					}
					break; /* see above comment */
				}
				if (modified) {
					break; /* the iterator fails otherwise */
				}
			}
		}
		if (abort && !toResolve.isEmpty()) {
			throw new ResolveDependenciesException(
					String.format("Nothing changed in this step."));
		}
		if (modified || !toResolve.isEmpty()) {
			this.resolve(ret, toResolve, resolved);
		}
	}

	/**
	 * This exception is thrown, when a dependency could not be resolved.
	 *
	 */
	public static class ResolveDependenciesException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ResolveDependenciesException(final String message) {
			super(message);
		}

		public ResolveDependenciesException(final String message, Throwable e) {
			super(message, e);
		}
	}

	/**
	 * This happens if operations that look up bundles in the data base could
	 * not find the desired bundle
	 *
	 */
	public static class BundleNotFoundException extends Exception {

		public BundleNotFoundException(String message) {
			super(message);
		}

		public BundleNotFoundException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 *
		 */
		private static final long serialVersionUID = -9216381783262697082L;
	}
}
