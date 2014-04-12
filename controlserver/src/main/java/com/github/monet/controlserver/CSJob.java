package com.github.monet.controlserver;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import com.github.monet.common.DBCollections;
import com.github.monet.common.logging.LogEvent;
import com.github.monet.common.logging.LoggingListener;
import com.github.monet.common.logging.LoggingPublisher;
import com.github.monet.worker.Job;
import com.github.monet.worker.JobState;
import com.github.monet.worker.WorkerJob;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * <p>
 * {@code CSJob} is short hand for <i>ControlServerJob</i>.
 * </p>
 *
 * <p>
 * It extends the {@link Job} class by providing the associated
 * {@link Experiment} and stores the {@link WorkerDescriptor} this {@code CSJob}
 * is assigned to.
 * </p>
 *
 * Note that the CSJob does not use {@link Job#getInputGraph()} or
 * {@link Job#getInputGraphPath()} - these are purely for the worker side.
 *
 * @author Marco Kuhnke, Andreas Pauly
 */
public class CSJob extends WorkerJob {
	/**
	 *
	 */
	private static final long serialVersionUID = -5540572630445432495L;
	private LoggingPublisher logPublisher;
	private Experiment parentExperiment;
	private WorkerDescriptor worker;
	private Date startedDate;
	private Date finishedDate;
	private CSLog log;

	/**
	 * Creates the CSJob based on a DBObject loaded from MongoDB.
	 *
	 * @param dbo
	 *            the DBObject describing the job
	 * @param parentExperiment
	 *            the parent Experiment
	 */
	@SuppressWarnings("unchecked")
	public CSJob(DBObject dbo, Experiment parentExperiment) {
		this(parentExperiment, (Map<String, Object>) dbo.get("metadata"));
		String id = (String) dbo.get("_id");
		int index = Integer.parseInt(id.substring(id.indexOf('/') + 1));
		this.setID(parentExperiment, index);
		String state = (String) dbo.get("state");
		this.startedDate = (Date) dbo.get("executedDatetime");
		this.finishedDate = (Date) dbo.get("finishedDatetime");
		this.state = Job.convertStateFromString(state);
		this.worker = WorkerDescriptor.getWorkers().get(dbo.get("worker"));
	}

	/**
	 * Constructor that extends the {@link Job} class by the parameter
	 * {@code parentExperiment}.
	 *
	 * @param parentExperiment
	 * @param hashmap
	 */
	public CSJob(Experiment parentExperiment, Map<String, Object> hashmap) {
		super(hashmap);
		this.parentExperiment = parentExperiment;
		this.worker = null;
		this.logPublisher = new LoggingPublisher();
		this.logPublisher.addLoggingListener(new LoggingListener() {
			/**
			 *
			 */
			private static final long serialVersionUID = 234858246440635508L;

			@Override
			public void logEvent(LogEvent event) {
				System.out.println(event.toString());
			}
		});
		this.addObserver(new Observer() {
			@Override
			public void update(Observable obs, Object o) {
				// if switching to INITIALIZING, set execution date
				JobState newState = (JobState) o;
				DB db = ControlServer.getInstance().db;
				DBCollection jobs = db.getCollection(DBCollections.JOBS);
				if (newState.equals(Job.State.INITIALIZING)) {
					startedDate = new Date();
					BasicDBObject pushObj = new BasicDBObject("$set",
							new BasicDBObject("executedDatetime", startedDate));
					jobs.update(new BasicDBObject("_id", getID()), pushObj);
				} else if (newState.isFinal()) {
					finishedDate = new Date();
					BasicDBObject pushObj = new BasicDBObject("$set",
							new BasicDBObject("finishedDatetime", finishedDate));
					jobs.update(new BasicDBObject("_id", getID()), pushObj);
				}
			}
		});
	}

	/**
	 * Sets the id of the job consisting of the parent Experiment's name and the
	 * index of the job within that Experiment.
	 *
	 * @param parentExeriment
	 *            the parent Experiment
	 * @param index
	 *            the index of the job within its Experiment
	 */
	private void setID(Experiment parentExperiment, int index) {
		this.parentExperiment = parentExperiment;
		String id = String.format("%s/%d", parentExperiment.getName(), index);
		super.metadata.put(KEY_JOB_ID, id);
		if (this.log != null) {
			this.deleteObserver(log);
			this.logPublisher.removeLoggingListener(log);
		}
		this.log = new CSLog(getID());
		this.addObserver(log);
		this.logPublisher.addLoggingListener(log);
	}

	/**
	 * Returns the parent {@link Experiment}.
	 *
	 * @return The parent experiment
	 */
	public Experiment getParentExperiment() {
		return this.parentExperiment;
	}

	/**
	 * Returns the Worker {@link WorkerDescriptor} this CSJob is assigned to.
	 *
	 * @return The Worker that processes this job
	 */
	public synchronized WorkerDescriptor getWorker() {
		return this.worker;
	}

	/**
	 * Assign the Worker that will process this Job. This is usually done by the
	 * {@link Scheduler}.
	 *
	 * @param worker
	 *            The Worker that will process this job
	 */
	public synchronized void setWorker(WorkerDescriptor worker) {
		this.worker = worker;
		// update object in MongoDB
		DB db = ControlServer.getInstance().db;
		DBCollection jobs = db.getCollection(DBCollections.JOBS);
		BasicDBObject pushObj = new BasicDBObject("$set", new BasicDBObject(
				"worker", worker.getName()));
		jobs.update(new BasicDBObject("_id", getID()), pushObj);
	}

	/**
	 * Get the publisher for any log events that might occur during the
	 * execution of the job on some worker.
	 *
	 * @return the logging publisher
	 */
	public LoggingPublisher getLogPublisher() {
		return this.logPublisher;
	}

	/**
	 * Returns the date and time when this job was executed. Note that the
	 * difference between this date and {@link #getFinishedDate()} will not be
	 * as accurate as {@link MeasuredData#getRuntimes()}.
	 *
	 * @return the date (and time) when this job was executed
	 */
	public Date getStartedDate() {
		return startedDate;
	}

	/**
	 * Returns the date and time when this job was finished. Note that the
	 * difference between this date and {@link #getStartedDate()()} will not be
	 * as accurate as {@link MeasuredData#getRuntimes()}.
	 *
	 * @return the date (and time) when this job was finished.
	 */
	public Date getFinishedDate() {
		return finishedDate;
	}

	/**
	 * Returns the log for this job.
	 *
	 * @return the log of this job
	 */
	public CSLog getLog() {
		return this.log;
	}

	/**
	 * Returns a {@link MeasuredData} object containing all measured data for
	 * this job.
	 *
	 * @return the MeasuredData object
	 */
	public MeasuredData getMeasuredData() {
		BasicDBObject query = new BasicDBObject("_id", this.getID());
		return new MeasuredData(query);
	}

	/**
	 * Writes the current state to MongoDB.
	 */
	private void writeStateToDB() {
		DB db = ControlServer.getInstance().db;
		DBCollection jobs = db.getCollection(DBCollections.JOBS);
		BasicDBObject pushObj = new BasicDBObject("$set", new BasicDBObject(
				"state", state.toString()));
		jobs.update(new BasicDBObject("_id", getID()), pushObj);
	}

	@Override
	public synchronized void setState(JobState newState) {
		super.setState(newState);
		this.writeStateToDB();
	}

	@Override
	public synchronized void setState(String newState) {
		super.setState(newState);
		this.writeStateToDB();
	}

	/**
	 * Provides the possibility to configure the CSJob. This is done by
	 * uploading a configuration file (xml).
	 */
	public void configureJob() {
		// FEATURE Parse the configuration xml
	}

	/**
	 * Tell the worker to cancel this job if it has already been started,
	 * otherwise just enter {@link Job#STATE_CANCELLED}.
	 */
	public synchronized void cancel() {
		ControlServer.getInstance().killJob(this);
	}

	/**
	 * Create a copy of this CSJob. Note that no checks are made if the new ID
	 * is valid.
	 *
	 * @param newIndex
	 *            the index of the copy
	 * @return the copy
	 */
	public CSJob createCopy(int newIndex) {
		@SuppressWarnings("unchecked")
		CSJob copy = new CSJob(
				this.parentExperiment,
				(HashMap<String, Object>) ((HashMap<String, Object>) this.metadata)
						.clone());
		copy.setID(this.parentExperiment, newIndex);
		return copy;
	}

	/**
	 * Create a copy of this CSJob for a new / copied Experiment. Note that no
	 * checks are made if the new ID is valid.
	 *
	 * @param newIndex
	 *            the index of the copy
	 * @param newParentExperiment
	 *            the new parent experiment
	 * @return the copy
	 */
	public CSJob createCopyWithNewExperiment(int newIndex,
			Experiment newParentExperiment) {
		@SuppressWarnings("unchecked")
		CSJob copy = new CSJob(
				newParentExperiment,
				(HashMap<String, Object>) ((HashMap<String, Object>) this.metadata)
						.clone());
		copy.setID(newParentExperiment, newIndex);
		return copy;
	}

	/**
	 * Throws a {@link ControlServerException} if the job violates the
	 * Experiment Contract according to a reference job.
	 *
	 * @param reference
	 *            the reference job
	 * @throws ControlServerException
	 *             if this job violates the experiment contract.
	 */
	public void obeysExperimentContract(CSJob reference)
			throws ControlServerException {
		// quickly check the size (also necessary to confirm that one job
		// doesn't have any parameters that the other one does
		int thisMetadataMissing = 0;
		if (!this.metadata.containsKey(KEY_JOB_ID) && reference.metadata.containsKey(KEY_JOB_ID)) {
			thisMetadataMissing++;
		}
		if (!this.metadata.containsKey(KEY_GRAPHFILE) && reference.metadata.containsKey(KEY_GRAPHFILE)) {
			thisMetadataMissing++;
		}
		if ((this.metadata.size() + thisMetadataMissing) != reference.metadata.size()) {
			throw new ControlServerException("Experiment Contract violated: Metadata map sizes do not match!");
		}
		// check all other parameters but ignore the graph file
		for (Entry<String, Object> e : this.metadata.entrySet()) {
			Object value = reference.metadata.get(e.getKey());
			if (e.getKey().equals(WorkerJob.KEY_GRAPHFILE)
					|| e.getKey().equals(WorkerJob.KEY_JOB_ID))
				// ignore the graph file and job id
				continue;
			else if (!value.equals(e.getValue()))
				throw new ControlServerException("Experiment Contract violated: Metadata entries '" + e.getKey() + "' does not match!");
		}
	}

	@Override
	public int hashCode() {
		return (this.parentExperiment.getName() + this.getID()).hashCode();
	}

	public void putParameter(String parameterKey, Object value) {
		this.metadata.put(parameterKey, value);
		DB db = ControlServer.getInstance().db;
		DBCollection jobs = db.getCollection(DBCollections.JOBS);
		BasicDBObject pushObj = new BasicDBObject("$set", new BasicDBObject(
				"metadata", this.getMetadata()));
		jobs.update(new BasicDBObject("_id", getID()), pushObj);
	}
}
