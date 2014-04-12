package com.github.monet.common;

/**
 * Constants for collections in the MongoDB.
 *
 * @author Max Günther
 */
public class DBCollections {

	/**
	 * Collection of all experiments.
	 *
	 * <p>
	 * Schema:
	 * <ul>
	 * <li><code>_id</code> - the name of the Experiment</li>
	 * <li><code>description</code> - description of the Experiment</li>
	 * <li><code>state</code> - the state of the Experiment</li>
	 * <li><code>priority</code> - the priority of the Experiment</li>
	 * <li><code>assignedWorkers</code> - a list of worker IDs that are part of
	 * this Experiment</li>
	 * </ul>
	 */
	public static final String EXPERIMENTS = "experiments";

	/**
	 * Collection of all jobs.
	 *
	 * <p>
	 * Schema:
	 * <ul>
	 * <li><code>_id</code> - ID of the job (consists of parent Experiment name
	 * and job index)</li>
	 * <li><code>state</code> - state of the job</li>
	 * <li><code>worker</code> - the worker the job is running on or was running
	 * on</li>
	 * <li><code>parentExperiment</code> - the name of the parent Experiment</li>
	 * <li><code>measuredData</code> - dictionary of all measured data</li>
	 * <li><code>log</code> - list of all {@link LogEvent}s</li>
	 * </ul>
	 */
	public static String JOBS = "jobs";

	/**
	 * Collection of all workers. This includes workers that are currently not
	 * connected to the ControlServer, but are somehow still known to it.
	 *
	 * <p>
	 * Schema:
	 * <ul>
	 * <li><code>_id</code> - name of the worker</li>
	 * <li><code>ip</code> - the last known IP of the worker</li>
	 * <li><code>port</code> - the last known port the worker operated on</li>
	 * <li><code>state</code> - the state of the worker</li>
	 * <li><code>ram</code> - the RAM of the worker in MiB</li>
	 * <li><code>cpu</code> - the CPu of the worker</li>
	 * </ul>
	 */
	public static final String WORKERS = "workers";

	/**
	 * Key for the interface->exporter data base collection used by the
	 * dependency manager.
	 */
	public static final String EXPORTERTABLE = "exportertable";

	/**
	 * Grid-FS collection that stores the actual bundle files together with
	 * basic meta-information such as the exported and imported packages.
	 */
	public static final String BUNDLE_FILES = "bundle_files";

	/**
	 * Grid-FS collection of graph input files.
	 */
	public static final String GRAPH_FILES = "graph_files";

	/**
	 * Grid-FS collection of measured files.
	 */
	public static final String MEASUREMENT_FILES = "measurement_files";

}
