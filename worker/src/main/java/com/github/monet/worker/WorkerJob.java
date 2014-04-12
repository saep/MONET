/**
 *
 */
package com.github.monet.worker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.common.DBCollections;
import com.github.monet.common.FileUtils;
import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

/**
 *
 */
public class WorkerJob extends Job {

	/**
	 *
	 */
	private static final long serialVersionUID = 971868315140298657L;

	/**
	 * Key to find the id of the job.
	 */
	public final static String KEY_JOB_ID = "jobid";

	/**
	 * Key to find the algorithm descriptor in the meta data hash map.
	 */
	public final static String KEY_ALGORITHM = "algorithm";

	/**
	 * Key to find the parser descriptor in the meta data hash map.
	 */
	public final static String KEY_GRAPHPARSER = "parser";

	/**
	 * Key to find the graph file name in mongodb in the meta data hash map.
	 */
	public final static String KEY_GRAPHFILE = "graph";

	/**
	 * Key to find the parameters in the meta data map.
	 */
	public final static String KEY_PARAMETERS = "parameters";

	/**
	 * Key to find the parser parameters in the meta data map.
	 */
	public static final Object KEY_PARSER_PARAMETERS = "parserparameters";

	/**
	 * The meta data of the job received by the control server.
	 */
	protected final Map<String, Object> metadata;

	/**
	 * The parsed graph instance.
	 */
	private volatile Object graph;

	/**
	 * The file path on the worker where the graph has been downloaded to.
	 */
	private File inputGraphFile;

	private String loggerName;

	/**
	 * Constructor.
	 */
	public WorkerJob(Map<String, Object> map) {
		super();
		this.metadata = map;
		this.graph = null;
		loggerName = String.format("job-logger-%s", getID());
	}

	@Override
	public String getID() {
		return (String) this.metadata.get(KEY_JOB_ID);
	}

	@Override
	public Logger getLogger() {
		return LogManager.getFormatterLogger(loggerName);
	}

	/**
	 * This method returns the descriptor of the algorithm.
	 *
	 * @return descriptor string of the algorithm
	 */
	public String getAlgorithmDescriptor() {
		assert (this.metadata.get(KEY_ALGORITHM) instanceof String) : KEY_ALGORITHM
				+ " descriptor is not a string!";
		String descriptor = (String) this.metadata.get(KEY_ALGORITHM);
		assert ((descriptor != null) && !descriptor.isEmpty()) : "No "
				+ KEY_ALGORITHM
				+ " descriptor found in the meta data of the job!";
		return descriptor;
	}

	@Override
	public String getParserDescriptor() {
		assert (this.metadata.get(KEY_GRAPHPARSER) instanceof String) : KEY_GRAPHPARSER
				+ " descriptor is not a string!";
		String descriptor = (String) this.metadata.get(KEY_GRAPHPARSER);
		assert ((descriptor != null) && !descriptor.isEmpty()) : "No "
				+ KEY_GRAPHPARSER
				+ " descriptor found in the meta data of the job!";
		return descriptor;
	}

	public String getGraphDescriptor() {
		assert (this.metadata.get(KEY_GRAPHFILE) instanceof String) : KEY_GRAPHFILE
				+ " descriptor is not a string!";
		String descriptor = (String) this.metadata.get(KEY_GRAPHFILE);
		assert ((descriptor != null) && !descriptor.isEmpty()) : "No "
				+ KEY_GRAPHFILE
				+ " descriptor found in the meta data of the job!";
		return descriptor;
	}

	@Override
	public Map<String, Object> getParameters() {
		@SuppressWarnings("unchecked")
		Map<String, Object> parameters = (Map<String, Object>) this.metadata
				.get(KEY_PARAMETERS);
		return parameters != null ? parameters : new HashMap<String, Object>();
	}

	/**
	 * This method returns the parameters designated for any component of the
	 * job, like the parser or some Service.
	 *
	 */
	public Map<String, Object> getParameters(String componentKey) {
		assert (this.metadata.get(componentKey) instanceof Map<?, ?>) : componentKey
				+ " are not a map!";
		Object tmp = metadata.get(componentKey);
		if (tmp instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> ret = (Map<String, Object>) tmp;
			return ret;
		} else {
			getLogger().error("Map for key: " + componentKey + " is not a map.");
			return new HashMap<>();
		}
	}

	/**
	 * This method returns the state of the job.
	 *
	 * @return the state of the job
	 */
	public JobState getState() {
		return super.getState();
	}

	/**
	 * This method returns the component keys of all components in this Job.
	 *
	 * @return
	 */
	public Iterable<String> getParameterizedComponents() {
		List<String> result = new LinkedList<String>();
		for (Map.Entry<String, Object> e : metadata.entrySet()) {
			if (e.getValue() != null) {
				result.add(e.getKey());
			}
		}
		return result;
	}

	/**
	 * Downloads a graph file to the system temporary file directory.
	 *
	 * The temporary file begins with "monet-input-graph-" followed by graphs ID
	 * in MongoDB and the suffix ".tmp".
	 *
	 * @param db
	 *            the MongoDB database to use
	 * @throws IOException
	 *             if any IO related errors occur while downloading or saving
	 *             the file
	 */
	public void downloadInputInstance(DB db) throws IOException {
		GridFS gridfs = new GridFS(db, DBCollections.GRAPH_FILES);
		final GridFSDBFile dbf = gridfs.findOne((String) this.metadata
				.get(KEY_GRAPHFILE));
		if (dbf == null) {
			throw new IOException(String.format(
					"could not find find input file '%s' on controlserver",
					this.metadata.get(KEY_GRAPHFILE)));
		}
		final InputStream is = dbf.getInputStream();
		inputGraphFile = FileUtils.createTempFileFromStream(
				String.format("monet-input-graph-%s",
						getID().replaceAll("/", "-")), ".graph", is);
	}

	/**
	 * This method returns the path to the input graph file.
	 *
	 * @return path to the input graph file
	 */
	public File getInputGraphPath() {
		// TODO is this used anywhere?
		return inputGraphFile;
	}

	@Override
	public Object getInputGraph() {
		assert (this.graph != null);
		return this.graph;
	}

	/**
	 * Sets the private graph variable to the given parsed input graph.
	 *
	 * @param parsed
	 *            input graph
	 */
	void setInputGraph(Object inputGraph) {
		assert (inputGraph != null);
		this.graph = inputGraph;
	}

	@Override
	protected void clean() {
		if ((inputGraphFile != null) && inputGraphFile.exists()) {
			inputGraphFile.delete();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getParserParameters() {
		Object tmp = metadata.get(KEY_PARSER_PARAMETERS);
		if (tmp instanceof Map) {
			return (Map<String, Object>) tmp;
		} else {
			getLogger().error("Uninitialized Parser parameter map");
			return new HashMap<String, Object>();
		}
	}

	/**
	 * This method returns the whole meta data map. It is recommended to use the
	 * particular methods to access single meta data information.
	 *
	 * @return The whole meta data map
	 */
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}
}
