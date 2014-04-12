package com.github.monet.controlserver;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.BasicBSONList;

import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.BundleValidationException;
import com.github.monet.common.Checksum;
import com.github.monet.common.Config;
import com.github.monet.common.DBCollections;
import com.github.monet.common.DependencyManager;
import com.github.monet.common.VersionedPackage;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

/**
 * The bundle manager is used to upload or delete bundles from the data base.
 */
public class BundleManager implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 7985591071511437146L;

	private static Logger LOG = LogManager.getLogger(BundleManager.class);

	/**
	 * The keys of the data base objects that must be set for a bundle.
	 */
	private static final Set<String> requiredFields;
	static {
		requiredFields = new TreeSet<>();
		requiredFields.addAll(Arrays.asList(//
				"imports", "exports", "hash"));
	}
	/**
	 * The bundle manager singleton instance.
	 */
	private static BundleManager instance;

	/**
	 * The data base this bundle manager is connected to.
	 */
	private final DB db;
	/**
	 * A dependency manager to ease dependency resolution.
	 */
	private final DependencyManager dm;
	/**
	 * A <tt>GridFS</tt> object which is used by MongoDB to store raw files.
	 */
	private final GridFS bundleFiles;

	/**
	 * Create a bundle manager from the configuration options.
	 */
	private BundleManager() {
		this(Config.getDBInstance(), DependencyManager.getInstance());
	}

	/**
	 * Create a bundle manager that simply initializes its variables
	 *
	 * @param db
	 *            the data base
	 * @param dm
	 *            the dependency manager
	 */
	private BundleManager(final DB db, DependencyManager dm) {
		this.db = db;
		this.dm = dm;
		this.bundleFiles = new GridFS(this.db, DBCollections.BUNDLE_FILES);
	}

	public static synchronized BundleManager getInstance() {
		if (instance == null) {
			instance = new BundleManager();
		}
		return instance;
	}

	/**
	 * This method uploads all bundles referenced by the input file.
	 * <p>
	 * If the file is a directory, then all bundles which are somewhere in that
	 * directory are being uploaded to the control server.<br>
	 * If the file is a regular file, then it will be uploaded directly to the
	 * control server.
	 *
	 * @param file
	 *            file or directory to upload to the control server
	 */
	void uploadBundleOrBundlesInDirectory(File file) {
		if (file.isDirectory()) {
			for (String f : file.list()) {
				this.uploadBundleOrBundlesInDirectory(new File(file, f));
			}
		} else if (file.isFile() && file.getName().endsWith(".jar")) {
			Map<String, String> m = upload(file);
			if (!m.isEmpty()) {
				LOG.warn("Could not upload bundle: " + file.getName());
				for (Entry<String, String> e : m.entrySet()) {
					LOG.warn(e.getKey() + ": " + e.getValue());
				}
			}
		}
	}

	/**
	 * This function removes a bundle completely from the control server.
	 * <p>
	 * This is practical because registered bundles can likely contain bugs that
	 * make them practically unsuited for usage. Also, newer bundles may just
	 * provide the same interfaces with a more efficient implementation and
	 * therefore it makes sense to simply start using the new bundle and remove
	 * the old one.
	 *
	 * @param descriptor
	 *            the bundle's identifier
	 */
	public void removeBundleFromDatabase(final BundleDescriptor descriptor) {
		removeBundleEntryInExporterTable(descriptor);
		this.bundleFiles.remove(descriptor.getDescriptor());
	}

	/**
	 * Try to upload a bundle to the control server.
	 * <p>
	 * If this operation fails in any way, then this function returns a map
	 * containing key words as well as a detailed error description. These can
	 * be used to create comprehensive error messages in the graphical user
	 * interface. The map may also contain multiple errors which makes it more
	 * pleasant to adjust those in one step rather than failing once for every
	 * error made.
	 * <p>
	 * XXX (saep) consider throwing a specialized exception rather than
	 * returning an errormap.
	 *
	 * @param bundleFile
	 *            the bundle file
	 * @return an error map (which ideally is empty)
	 */
	public Map<String, String> upload(final File bundleFile) {

		JarFile jar = null;
		Map<String, String> m = null;
		try {
			jar = new JarFile(bundleFile);
			ManifestParser p = new ManifestParser(jar);
			m = p.missingFields();
			if (!m.isEmpty()) {
				return m;
			}
			Map<String, Serializable> bundleAttributes = new TreeMap<>();
			final String hash = Checksum.sha256sum(bundleFile);
			bundleAttributes.put("hash", hash);
			final Manifest manifest = jar.getManifest();
			final BundleDescriptor descriptor = BundleDescriptor.fromManifest(
					manifest, null);
			BasicDBList dbexports = new BasicDBList();
			Set<VersionedPackage> exps = p.retrieveExports();
			for (VersionedPackage e : exps) {
				DBObject tmp = new BasicDBObject(2);
				tmp.put("name", e.getName());
				tmp.put("version", e.getVersion().show());
				dbexports.add(tmp);
			}
			bundleAttributes.put("exports", dbexports);
			BasicDBList dbimports = new BasicDBList();
			Set<VersionedPackage> imps = p.retrieveImports();
			for (VersionedPackage i : imps) {
				BasicDBObject tmp = new BasicDBObject(2);
				tmp.put("name", i.getName());
				tmp.put("version", i.getVersion().show());
				dbimports.add(tmp);
			}
			bundleAttributes.put("imports", dbimports);

			// upload
			String algType = p.getManifest().getMainAttributes()
					.getValue("Algorithm-Type");
			String type = p.getManifest().getMainAttributes().getValue("Type");
			// Algorithm specific block
			if ((algType != null) && (algType.length() > 0)) {
				bundleAttributes.put("Algorithm-Type", algType);
				String arity = p.getManifest().getMainAttributes()
						.getValue("Arity");
				if ((arity != null) && (arity.length() > 0)) {
					bundleAttributes.put("Arity", Integer.parseInt(arity));
				} else {
					bundleAttributes.put("Arity", 0);
				}
				bundleAttributes.put("type", "algorithm");
				new AlgorithmParameterIterator(bundleFile);
			} else if (type != null) {
				type = type.toLowerCase();
				switch (type) {
				case "graph":
					new ParserParameterIterator(bundleFile);
				case "parser":
					bundleAttributes.put("type", type);
					break;
				default:
					LOG.warn("Unknown type for bundle: "
							+ descriptor.getDescriptor());
				}
			}
			uncheckedUpload(bundleFile, descriptor, exps, bundleAttributes);
		} catch (BundleValidationException bve) {
			m.put("Bundle validation failed", bve.getMessage());
		} catch (ParseException pe) {
			m.put("Parsing of a field failed.", pe.getLocalizedMessage());
		} catch (NoSuchAlgorithmException e) {
			m.put("Hash", "The hash algorithm was not found, this is probably "
					+ "an incompability with the virtual machine and "
					+ "has not been experienced yet.");
		} catch (IOException e) {
			if (m == null) {
				m = new TreeMap<>();
			}
			m.put("IOException", "The bundle file to upload caused an IO error");
			m.put("Message", e.getMessage());
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					// I mean... seriously?
					m.put("IOException", "The bundle jar could not be closed");
					m.put("Message", e.getMessage());
				}
			}
		}
		return m;
	}

	/**
	 * Extracted download method to make tests possible without haven a lot of
	 * bundles in the resources folder.
	 *
	 * @param bundleFile
	 *            the actual jar file
	 * @param descriptor
	 *            the bundle descriptor
	 * @param exps
	 *            a set of package names the uploaded bundle exports
	 * @param bundleAttributes
	 *            all attributes defined in {@link BundleManager#requiredFields}
	 *            must be set
	 * @throws IOException
	 * @throws BundleValidationException
	 */
	void uncheckedUpload(final File bundleFile,
			final BundleDescriptor descriptor, Set<VersionedPackage> exps,
			Map<String, Serializable> bundleAttributes) throws IOException,
			BundleValidationException {
		this.bundleFiles.remove(descriptor.getDescriptor());
		final GridFSInputFile mongoFile = this.bundleFiles
				.createFile(bundleFile);

		if (!bundleAttributes.keySet().containsAll(requiredFields)) {
			TreeSet<String> missing = new TreeSet<>(requiredFields);
			missing.removeAll(bundleAttributes.keySet());
			throw new BundleValidationException(
					"Not all required fields for a bundle are defined: "
							+ missing.toString());
		}
		for (Entry<String, Serializable> e : bundleAttributes.entrySet()) {
			mongoFile.put(e.getKey(), e.getValue());
		}
		mongoFile.setFilename(descriptor.getDescriptor());
		mongoFile.save();
		mongoFile.validate();
		updateExporter(descriptor, exps);
		LOG.debug("Successfully upload bundle: " + bundleFile.getName());
	}

	/**
	 * Update the exported package names of the given bundle.
	 * <p>
	 * If the entry for the package already exists, it is overwritten with the
	 * new value(s).
	 */
	private void updateExporter(final BundleDescriptor descriptor,
			final Set<VersionedPackage> exps) {
		DBCollection coll = this.db.getCollection(DBCollections.EXPORTERTABLE);
		for (VersionedPackage iface : exps) {
			Set<BundleDescriptor> presentProviders = dm.getProviders(iface
					.getName());
			if (presentProviders.contains(descriptor)) {
				break;
			}
			DBObject entry = coll.findOne(iface.getName());
			if (entry == null) {
				DBObject bundleEntries = new BasicDBObject();
				BasicBSONList bundleVersions = new BasicDBList();
				bundleVersions.add(descriptor.getVersion().show());
				bundleEntries.put(descriptor.getName(), bundleVersions);
				entry = BasicDBObjectBuilder.start()
						.add("_id", iface.getName())
						.append(DBCollections.BUNDLE_FILES, bundleEntries)
						.get();
				coll.insert(entry);
			} else {
				this.createUpdatedProviderEntry(coll, entry, descriptor);
			}
		}
	}

	/**
	 * Update the data base object for the old entry and replace it with a new
	 * and updated one.
	 *
	 * @param coll
	 *            the DBCollection in which the old entry resides
	 * @param old
	 *            the old entry
	 * @param descriptor
	 *            the bundle descriptor which should be added to the old entry
	 */
	private void createUpdatedProviderEntry(DBCollection coll, DBObject old,
			BundleDescriptor descriptor) {
		coll.remove(old);
		Object bundles = old.get(DBCollections.BUNDLE_FILES);
		if ((bundles != null) && (bundles instanceof DBObject)) {
			DBObject bs = (DBObject) bundles;
			Object oldVersions = ((DBObject) bundles).get(descriptor.getName());
			if ((oldVersions instanceof Collection<?>)
					&& (!((Collection<?>) oldVersions).contains(descriptor
							.getVersion()))) {
				BasicBSONList newVersions = new BasicDBList();
				newVersions.addAll((Collection<?>) oldVersions);
				newVersions.add(descriptor.getVersion().show());
				bs.removeField(descriptor.getName());
				bs.put(descriptor.getName(), newVersions);
			}
			coll.insert(old);
		}
	}

	/**
	 * This function removes all entries from the data base that the given
	 * bundle has exported.
	 *
	 * @param descriptor
	 *            the bundle descriptor
	 */
	private void removeBundleEntryInExporterTable(
			final BundleDescriptor descriptor) {
		DBObject b = this.bundleFiles.findOne(descriptor.getDescriptor());
		if (b == null) {
			return;
		}
		Object exports = b.get("exports");
		if ((exports != null) && (exports instanceof Collection<?>)) {
			for (Object export : (Collection<?>) exports) {
				if (export instanceof String) {
					removeBundleFromExportTable(descriptor, (String) export);
				}
			}
		}
	}

	/**
	 * Remove the bundle descriptor (or just the version) from the provider data
	 * base collection.
	 *
	 * @param descriptor
	 *            the bundle descriptor
	 * @param packageName
	 *            the package name
	 */
	private void removeBundleFromExportTable(final BundleDescriptor descriptor,
			final String packageName) {
		DBCollection coll = this.db.getCollection(DBCollections.EXPORTERTABLE);
		DBObject entry = coll.findOne(packageName);
		if (entry == null) {
			return;
		}
		Object bundles = entry.get("bundles");
		if ((bundles != null) && (bundles instanceof DBObject)) {
			Object versions = ((DBObject) bundles).get(descriptor.getName());
			if (versions instanceof BasicBSONList) {
				((Collection<?>) versions).remove(descriptor.getVersion());
			}
			coll.remove(new BasicDBObject("_id", packageName));
			// necessary
			((DBObject) bundles).removeField(descriptor.getName());
			if (!((BasicBSONList) versions).isEmpty()) {
				((DBObject) bundles).put(descriptor.getName(), versions);
			}
			entry.removeField("bundles");
			if (!((DBObject) bundles).keySet().isEmpty()) {
				entry.put("bundles", bundles);
				coll.insert(entry);
			}
		} else {
			LOG.warn(String.format(
					"Inconsistent data base entries for the provider's "
							+ "interfaceName: %s", packageName));
		}
	}

	/**
	 * This function is dangerous!
	 * <p>
	 * It connects to the currently configured mongodb database and deletes all
	 * collections which have something to do with bundles.
	 */
	void deleteBundles() {
		DBCollection bfCollection = db
				.getCollection(DBCollections.BUNDLE_FILES);
		bfCollection.getCollection("files").drop();
		bfCollection.getCollection("chunks").drop();
		db.getCollection(DBCollections.EXPORTERTABLE).drop();
	}

}
