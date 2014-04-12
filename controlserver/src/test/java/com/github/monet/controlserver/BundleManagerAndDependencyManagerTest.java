package com.github.monet.controlserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.BundleValidationException;
import com.github.monet.common.Checksum;
import com.github.monet.common.Config;
import com.github.monet.common.DependencyManager;
import com.github.monet.common.VersionedPackage;
import com.github.monet.controlserver.BundleManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class BundleManagerAndDependencyManagerTest {

	private static class BundleMetaData {
		public BundleMetaData(String name, String version) {
			BundleDescriptor tmp = null;
			try {
				tmp = new BundleDescriptor(name, version);
			} catch (BundleValidationException e) {
				// will throw errors in the tests
			}
			this.bd = tmp;
			this.imports = new TreeSet<>();
			this.exports = new TreeSet<>();
			exports.add(bd);
		}

		public final BundleDescriptor bd;
		public final Set<VersionedPackage> imports;
		public final Set<VersionedPackage> exports;

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(bd.getName()).append(" (").append(bd.getVersion())
					.append(")\n");
			sb.append("Exports: ").append(exports.toString()).append("\n");
			sb.append("Imports: ").append(imports.toString()).append("\n");
			return sb.toString();
		}
	}

	private static final DependencyManager dm = DependencyManager.getInstance();
	private static final BundleManager bm = BundleManager.getInstance();

	/* Some bundle descriptors. */
	private static BundleMetaData aLeaf1;
	private static BundleMetaData aLeaf2;
	private static BundleMetaData bLeaf;
	private static BundleMetaData cLeaf;
	private static BundleMetaData dLeaf1;
	private static BundleMetaData dLeaf2;
	private static BundleMetaData dLeaf3;
	private static BundleMetaData root1;
	private static BundleMetaData root2;
	private static BundleMetaData aNode1;
	private static BundleMetaData aNode2;
	private static BundleMetaData bNode3;
	private static BundleMetaData aCircularNode;
	private static BundleMetaData bCircularNode;
	private static BundleMetaData cCircularNode;
	private static Set<BundleMetaData> allBundles = new HashSet<>();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		new File(Config.getInstance().getBundleCacheDir()).mkdirs();

		aLeaf1 = new BundleMetaData("aLeaf", "1.0");
		aLeaf2 = new BundleMetaData("aLeaf", "2.0");
		bLeaf = new BundleMetaData("bLeaf", "1.0");
		cLeaf = new BundleMetaData("cLeaf", "1.0");
		dLeaf1 = new BundleMetaData("dLeaf", "1.0");
		dLeaf2 = new BundleMetaData("dLeaf", "2.0");
		dLeaf3 = new BundleMetaData("dLeaf", "3.0");
		aNode1 = new BundleMetaData("anode", "1.0");
		aNode1.imports.add(aLeaf1.bd);
		aNode1.imports.add(bLeaf.bd);
		aNode1.imports.add(dLeaf1.bd);
		aNode2 = new BundleMetaData("anode", "2.0");
		aNode2.imports.add(aLeaf2.bd);
		aNode2.imports.add(cLeaf.bd);
		aNode2.imports.add(dLeaf2.bd);
		bNode3 = new BundleMetaData("bnode", "3.0");
		bNode3.imports.add(bLeaf.bd);
		bNode3.imports.add(cLeaf.bd);
		bNode3.imports.add(dLeaf3.bd);
		root1 = new BundleMetaData("root", "1.0");
		root1.imports.add(aNode1.bd);
		root2 = new BundleMetaData("root", "2.0");
		root2.imports.add(aNode2.bd);
		root2.imports.add(bNode3.bd);
		aCircularNode = new BundleMetaData("aCirc", "1.0");
		bCircularNode = new BundleMetaData("bCirc", "1.0");
		cCircularNode = new BundleMetaData("cCirc", "1.0");
		cCircularNode.imports.add(cCircularNode.bd);
		aCircularNode.imports.add(bCircularNode.bd);
		bCircularNode.imports.add(bCircularNode.bd);

		allBundles.addAll(Arrays.asList(aLeaf1, aLeaf2, bLeaf, cLeaf, dLeaf1,
				dLeaf2, dLeaf3,//
				aNode1, aNode2, bNode3,//
				aCircularNode, bCircularNode, cCircularNode,//
				root1, root2));

		for (BundleMetaData meta : allBundles) {
			uploadToDatabase(meta);
		}

	}

	private static void uploadToDatabase(BundleMetaData meta)
			throws IOException, NoSuchAlgorithmException,
			BundleValidationException {
		Map<String, Serializable> atts = new TreeMap<>();
		File tmpFile = makeRandomFile(meta.bd);
		atts.put("hash", Checksum.sha256sum(tmpFile));
		atts.put("imports", toDBList(meta.imports));
		atts.put("exports", toDBList(meta.exports));
		bm.uncheckedUpload(tmpFile, meta.bd, meta.exports, atts);
	}

	private static BasicDBList toDBList(Set<VersionedPackage> packages) {
		BasicDBList dblist = new BasicDBList();
		for (VersionedPackage p : packages) {
			DBObject tmp = new BasicDBObject(2);
			tmp.put("name", p.getName());
			tmp.put("version", p.getVersion().show());
			dblist.add(tmp);
		}
		return dblist;
	}

	private static File makeRandomFile(BundleDescriptor descriptor)
			throws IOException {
		Random r = new Random();
		File rand = File.createTempFile("bundle", ".jar");
		FileOutputStream os = new FileOutputStream(rand);
		int bytes = r.nextInt(1024);
		byte[] buffer = new byte[bytes];
		r.nextBytes(buffer);
		os.write(buffer);
		os.close();
		File ret = new File(Config.getInstance().getBundleCacheDir(),
				descriptor.getCleanJarName());
		if (rand.renameTo(ret)) {
			ret.deleteOnExit();
		} else {
			rand.deleteOnExit();
		}
		return ret;
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		bm.deleteBundles();
	}

	@Before
	public void setUp() throws Exception {
	}

	public void testGetProviders() {
		assertEquals(2, dm.getProviders("aLeaf").size());
		assertEquals(1, dm.getProviders("bLeaf").size());
		assertEquals(1, dm.getProviders("cLeaf").size());
		assertEquals(3, dm.getProviders("dLeaf").size());
		assertEquals(2, dm.getProviders("anode").size());
		assertEquals(1, dm.getProviders("bnode").size());
		assertEquals(2, dm.getProviders("root").size());
		assertEquals(1, dm.getProviders("aCirc").size());
		assertEquals(1, dm.getProviders("bCirc").size());
		assertEquals(1, dm.getProviders("cCirc").size());
	}

	public void testExoprtedBy() {
		Collection<BundleDescriptor> aleaf1exp = dm.exportedBy(aLeaf1.bd);
		assertEquals(1, aleaf1exp.size());
		assertTrue(aleaf1exp.contains(aLeaf1.bd));
		Collection<BundleDescriptor> bLeafexp = dm.exportedBy(bLeaf.bd);
		assertEquals(1, bLeafexp.size());
		assertTrue(bLeafexp.contains(bLeaf.bd));
		Collection<BundleDescriptor> cleafexp = dm.exportedBy(cLeaf.bd);
		assertEquals(1, cleafexp.size());
		assertTrue(cleafexp.contains(cLeaf.bd));
		Collection<BundleDescriptor> dleaf1exp = dm.exportedBy(dLeaf1.bd);
		assertEquals(1, dleaf1exp.size());
		assertTrue(dleaf1exp.contains(dLeaf1.bd));
		Collection<BundleDescriptor> dleaf2exp = dm.exportedBy(dLeaf2.bd);
		assertEquals(1, dleaf2exp.size());
		assertTrue(dleaf2exp.contains(dLeaf2.bd));
		Collection<BundleDescriptor> dleaf3exp = dm.exportedBy(dLeaf3.bd);
		assertEquals(1, dleaf3exp.size());
		assertTrue(dleaf3exp.contains(dLeaf3.bd));
		Collection<BundleDescriptor> anode1exp = dm.exportedBy(aNode1.bd);
		assertEquals(1, anode1exp.size());
		assertTrue(anode1exp.contains(aNode1.bd));
		Collection<BundleDescriptor> anode2exp = dm.exportedBy(aNode2.bd);
		assertEquals(1, anode2exp.size());
		assertTrue(anode2exp.contains(aNode2.bd));
		Collection<BundleDescriptor> anode3exp = dm.exportedBy(bNode3.bd);
		assertEquals(1, anode3exp.size());
		assertTrue(anode3exp.contains(bNode3.bd));
		Collection<BundleDescriptor> root1exp = dm.exportedBy(root1.bd);
		assertEquals(1, root1exp.size());
		assertTrue(root1exp.contains(root1.bd));
		Collection<BundleDescriptor> root2exp = dm.exportedBy(root2.bd);
		assertEquals(1, root2exp.size());
		assertTrue(root2exp.contains(root2.bd));
		Collection<BundleDescriptor> acircexp = dm.exportedBy(aCircularNode.bd);
		assertEquals(1, acircexp.size());
		assertTrue(acircexp.contains(aCircularNode.bd));
		Collection<BundleDescriptor> bcircexp = dm.exportedBy(bCircularNode.bd);
		assertEquals(1, bcircexp.size());
		assertTrue(bcircexp.contains(bCircularNode.bd));
		Collection<BundleDescriptor> ccircexp = dm.exportedBy(cCircularNode.bd);
		assertEquals(1, ccircexp.size());
		assertTrue(ccircexp.contains(cCircularNode.bd));
	}

}
