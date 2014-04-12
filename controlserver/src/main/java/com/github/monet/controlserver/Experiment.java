package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.common.Config;
import com.github.monet.common.DBCollections;
import com.github.monet.worker.Job;
import com.github.monet.worker.JobState;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * An experiment as created by an user. One Experiment is a collection of jobs
 * to be executed on one or more workers.
 *
 * All jobs of one Experiment execute the same algorithm (in name and version)
 * and parameters, but may differ in the input instance. This is called the
 * Experiment Contract.
 *
 * The user may assign a number of workers that the experiment must use
 * exclusively. If no workers are assigned to an experiment the
 * {@link Scheduler} may use any worker.
 *
 * The exact state of an Experiment is highly complex as all jobs have their own
 * state. The Experiment has its own state that abstracts the states of all its
 * jobs. If this state changes any observers of an Experiments are notified with
 * the new state.
 *
 * In order to commit changes made to an Experiment you need to call its
 * {@link #save()} method. This method should be called whenever you have
 * changed any number of properties and won't change any more properties for the
 * moment.
 *
 * @author Max Günther
 */
public class Experiment extends Observable implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -8838765611604200656L;
	/**
	 * The logger for this experiment
	 */
	private static Logger LOG = LogManager.getFormatterLogger(Experiment.class);
	/**
	 * The Observer for all jobs of this Experiment.
	 */
	private JobObserver jobObserver;
	/**
	 * A list of workers that the user assigned to this Experiment. If this list
	 * is not empty, the scheduler must only use workers within this list. If
	 * none of the workers in this list is currently available, this Experiment
	 * must rest.
	 */
	private List<WorkerDescriptor> assignedWorkers;
	/**
	 * The name of the Experiment. Must be unique across all Experiments.
	 */
	private String name;
	/**
	 * The description of the Experiment. Short informational text to give the
	 * user an idea of this experiments details.
	 */
	private String description;
	/**
	 * The list of jobs that make up this Experiment. These jobs must all
	 * execute the same algorithm (in name and version).
	 */
	private List<CSJob> jobs;
	/**
	 * For creating incremental job IDs.
	 */
	private int jobIDCounter = 1;
	/**
	 * List of all jobs in this Experiment that have been finished.
	 *
	 * @see Job#FINISH_STATES
	 */
	private List<CSJob> finishedJobs;
	/**
	 * List of all jobs in this Experiment that are running. These are states
	 * that aren't in {@link Job#FINISH_STATES} or {@link Job#STATE_NEW}.
	 */
	private List<CSJob> runningJobs;
	/**
	 * List of all jobs in this Experiment that haven't been started (@link
	 * {@link Job#STATE_NEW} .
	 */
	private List<CSJob> unstartedJobs;
	/**
	 * The state of the Experiment. The state is changed by the
	 * {@link Scheduler}.
	 */
	private String state;
	/**
	 * The lower the {@code priority}, the earlier this experiment will be
	 * processed.
	 */
	private int priority;
	/**
	 * The number of jobs that were initialized at some point. They may,
	 * however, have never reached a running state.
	 */
	private int initialized_count = 0;
	/**
	 * The number of jobs that have been scheduled by the {@link Scheduler}, but
	 * the worker hasn't confirmed that they are initializing.
	 */
	private int scheduled_count = 0;
	/**
	 * The number of jobs that were running at some time (regardless of
	 * success).
	 */
	private int processed_count;
	/**
	 * The number of jobs that have successfully finished.
	 */
	private int success_count;
	/**
	 * The number of jobs that have failed.
	 */
	private int failed_count;
	/**
	 * The number of jobs that have been cancelled.
	 */
	private int cancelled_count;
	/**
	 * The number of jobs that have been aborted by a timer limit.
	 */
	private int aborted_count;

	/**
	 * The date and time when this experiment was created.
	 */
	private Date createdDate;
	/**
	 * The date and time when this experiment was started.
	 */
	private Date startedDate;
	/**
	 * The date and time when this experiment was finished.
	 */
	private Date finishedDate;

	/**
	 * The algorithm and parameters of all jobs that belong to this experiment.
	 */
	private Map<String, Object> map;

	/**
	 * The reference to the original experiment, if this is a repeat.
	 */
	private Experiment repeatOf;

	/**
	 * Map of all Experiments;
	 */
	private static Map<String, Experiment> experiments = new HashMap<>();

	/**
	 * Indicates that an experiment is ready to be run.
	 * <p>
	 * This means that it cannot be modified anymore and will be scheduled as
	 * soon as possible.
	 */
	public static final String STATE_READY = "READY";

	/**
	 * The Experiment has just been created. Only during this state jobs may be
	 * added to and removed from the Experiment. Workers however, may be
	 * assigned to an Experiment even as it is running.
	 */
	public final static String STATE_NEW = "new";
	/**
	 * One or more of the Experiments jobs are being executed.
	 */
	public final static String STATE_ACTIVE = "active";
	/**
	 * None of the jobs are currently being executed and they are waiting to be
	 * scheduled.
	 */
	public final static String STATE_WAITING = "waiting";
	/**
	 * The Experiment is being cancelled, but some jobs have not stopped yet.
	 */
	public final static String STATE_CANCELLING = "cancelling";
	/**
	 * The Experiment has been cancelled. None of the jobs of this Experiment is
	 * running.
	 */
	public final static String STATE_CANCELLED = "cancelled";
	/**
	 * The Experiment is being paused, but some jobs haven't terminated yet.
	 */
	public final static String STATE_PAUSING = "pausing";
	/**
	 * The Experiment has been paused. None of the jobs of this Experiment is
	 * running.
	 */
	public final static String STATE_PAUSED = "paused";
	/**
	 * All Experiments were executed and at least one job failed and at least
	 * one job was successfully executed.
	 */
	public final static String STATE_PARTIAL_SUCCESS = "partial success";
	/**
	 * The Experiment has been executed successfully, meaning all jobs were
	 * successfully executed, not counting jobs that were cancelled or aborted.
	 * However it is considered a failure if all jobs have been cancelled or
	 * aborted.
	 */
	public final static String STATE_SUCCESS = "success";
	/**
	 * The Experiment failed, meaning no jobs have succeeded.
	 */
	public final static String STATE_FAILED = "failed";
	/**
	 * When an Experiment is in one of these states it is considered finished:
	 * {@link #STATE_CANCELLED}, {@link #STATE_FAILED},
	 * {@link #STATE_PARTIAL_SUCCESS} and {@link #STATE_SUCCESS}.
	 */
	public final static Set<String> FINISH_STATES;

	static {
		Set<String> finishStates = new HashSet<>();
		finishStates.add(STATE_CANCELLED);
		finishStates.add(STATE_FAILED);
		finishStates.add(STATE_PARTIAL_SUCCESS);
		finishStates.add(STATE_SUCCESS);
		FINISH_STATES = finishStates;

		// retrieve Experiments from DB
		DB db = Config.getDBInstance();
		DBCollection experiments = db.getCollection(DBCollections.EXPERIMENTS);
		for (DBObject dbo : experiments.find()) {
			try {
				new Experiment(dbo);
			} catch (ControlServerException e) {
				// this should never occur
				System.err.println("inconsistent experiments in database");
			}
		}
	}

	private class ExperimentObserver implements Observer, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 7939637605733856099L;

		@Override
		public void update(Observable obs, Object o) {
			if (o instanceof PriorityChangedEvent) {
				return;
			} else if (o instanceof StateChangedEvent) {
				// update start and finish datetimes
				StateChangedEvent event = (StateChangedEvent) o;
				DB db = ControlServer.getInstance().db;
				DBCollection exps = db.getCollection(DBCollections.EXPERIMENTS);
				if (FINISH_STATES.contains(event.state)) {
					finishedDate = new Date();
					BasicDBObject pushObj = new BasicDBObject("$set",
							new BasicDBObject("finishedDatetime", finishedDate));
					exps.update(new BasicDBObject("_id", getName()), pushObj);
				} else if (event.state.equalsIgnoreCase(STATE_ACTIVE)) {
					startedDate = new Date();
					BasicDBObject pushObj = new BasicDBObject("$set",
							new BasicDBObject("startedDatetime", startedDate));
					exps.update(new BasicDBObject("_id", getName()), pushObj);
				} else if (event.state.equalsIgnoreCase(STATE_READY)) {
					try {
						ControlServer.getScheduler().addExperiment(
								Experiment.this);
					} catch (ControlServerException e) {
						/* Should not happen. */
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				} else if (event.state.equalsIgnoreCase(STATE_CANCELLING)) {

				}
			}
		}
	}

	private Experiment() {
		this.assignedWorkers = new ArrayList<WorkerDescriptor>();
		this.finishedJobs = new ArrayList<CSJob>();
		this.unstartedJobs = new ArrayList<CSJob>();
		this.runningJobs = new ArrayList<CSJob>();
		this.jobs = new ArrayList<CSJob>();
		this.map = new HashMap<String, Object>(2);
		this.jobObserver = new JobObserver();
		this.createdDate = new Date();
		this.addObserver(new ExperimentObserver());
	}

	@SuppressWarnings("unchecked")
	private Experiment(DBObject dbo) throws ControlServerException {
		this();
		try {
			this.name = (String) dbo.get("_id");
			this.state = (String) dbo.get("state");
			this.description = (String) dbo.get("description");
			this.priority = (int) dbo.get("priority");
			this.createdDate = (Date) dbo.get("createdDatetime");
			this.startedDate = (Date) dbo.get("startedDatetime");
			this.finishedDate = (Date) dbo.get("finishedDatetime");
			Map<String, Object> loadedMap = (Map<String, Object>) dbo
					.get("map");
			if (loadedMap != null) {
				this.map = loadedMap;
			}
			for (String workerName : (List<String>) dbo.get("assignedWorkers")) {
				// lookup assigned workers
				WorkerDescriptor wd = WorkerDescriptor.getWorkers().get(
						workerName);
				if (wd == null) {
					LOG.error("the worker %s is missing", this.name);
				} else {
					this.assignedWorkers.add(wd);
				}
			}
			String repeatOfName = (String) dbo.get("repeatOf");
			synchronized (experiments) {
				if (experiments.containsKey(this.name)) {
					throw new ControlServerException(String.format(
							"an experiment with name '%s' already exists",
							this.name));
				}
				experiments.put(this.name, this);
				if (experiments.containsKey(repeatOfName)) {
					this.repeatOf = experiments.get(repeatOfName);
				}
			}
			loadJobs();
			if (state.equalsIgnoreCase(STATE_READY)) {
				ControlServer.getScheduler().addExperiment(this);
			}
		} catch (NullPointerException npe) {
			return;
		}
	}

	/**
	 * Constructor that takes the {@code name} of the Experiment.
	 *
	 * @param name
	 *            the unique name of the Experiment
	 * @throws ControlServerException
	 *             if an Experiment with that name already exists
	 */
	public Experiment(String name, String description)
			throws ControlServerException {
		this();
		this.name = name;
		this.description = description;
		this.priority = Integer.MAX_VALUE;
		this.state = STATE_NEW;

		synchronized (experiments) {
			if (experiments.containsKey(this.name)) {
				throw new ControlServerException(String.format(
						"an experiment with name '%s' already exists",
						this.name));
			}
			experiments.put(this.name, this);
		}

		insertExperimentIntoDB();
	}

	/**
	 * Constructor that creates a copy of an other experiment.
	 *
	 * @param repeatOf
	 *            The other experiment to create the copy from
	 */
	@SuppressWarnings("unchecked")
	public Experiment(Experiment repeatOfExp) throws ControlServerException {
		this();
		// Climb up the repeat of ladder to find the original experiment
		while (repeatOfExp.getRepeatOf() != null) {
			repeatOfExp = repeatOfExp.getRepeatOf();
		}
		// Find a unused copy name
		int repeatCounter = 1;
		String repeatOfName = "Repetition " + repeatCounter + " of "
				+ repeatOfExp.getName();
		while (experiments.containsKey(repeatOfName)) {
			repeatCounter++;
			repeatOfName = "Repetition " + repeatCounter + " of "
					+ repeatOfExp.getName();
		}
		// Copy the values
		this.name = repeatOfName;
		this.state = STATE_NEW;
		this.description = repeatOfExp.getDescription();
		this.priority = repeatOfExp.getPriority();
		this.assignedWorkers = repeatOfExp.getAssignedWorkers();
		synchronized (experiments) {
			if (experiments.containsKey(this.name)) {
				throw new ControlServerException(String.format(
						"an experiment with name '%s' already exists",
						this.name));
			}
			experiments.put(this.name, this);
		}
		this.repeatOf = repeatOfExp;
		for (CSJob job : repeatOfExp.getJobs()) {
			String oldId = job.getID();
			int oldIdInt = Integer
					.parseInt(oldId.substring(oldId.indexOf('/') + 1));
			CSJob newJob = job.createCopyWithNewExperiment(oldIdInt, this);
			LOG.log(Level.INFO, newJob.getID());
			this.addJobs(newJob, 1);
		}
		this.map = (HashMap<String, Object>) ((HashMap<String, Object>) repeatOfExp
				.getHashmap()).clone();
		insertExperimentIntoDB();
	}

	private void insertExperimentIntoDB() {
		// insert Experiment into MongoDB
		DB db = ControlServer.getInstance().db;
		DBCollection experiments = db.getCollection(DBCollections.EXPERIMENTS);
		BasicDBObject exp = new BasicDBObject();
		exp.put("_id", this.getName());
		exp.put("state", getState());
		exp.put("description", getDescription());
		exp.put("priority", getPriority());
		exp.put("assignedWorkers", new BasicDBList());
		exp.put("startedDatetime", null);
		exp.put("createdDatetime", createdDate);
		exp.put("finishedDatetime", null);
		if (repeatOf != null) {
			exp.put("repeatOf", repeatOf.getName());
		}
		experiments.insert(exp);
	}

	/**
	 * Returns the name of the Experiment. You may not change the name of the
	 * Experiment once it has been created.
	 *
	 * @return the name of the Experiment
	 */
	public synchronized String getName() {
		return this.name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
		// update object in MongoDB
		DB db = ControlServer.getInstance().db;
		DBCollection experiments = db.getCollection(DBCollections.EXPERIMENTS);
		BasicDBObject pushObj = new BasicDBObject("$set", new BasicDBObject(
				"description", description));
		experiments.update(new BasicDBObject("_id", getName()), pushObj);
	}

	/**
	 * Returns the priority of the Experiment. The {@link Scheduler} will you
	 * this priority to schedule the {@link CSJob}s of an Experiment. A priority
	 * of 0 is the highest priority.
	 *
	 * @return the priority of this Experiment
	 */
	public synchronized int getPriority() {
		return this.priority;
	}

	/**
	 * Set the priority of this Experiment. Any observers will be notified of
	 * this change via a {@link PriorityChangedEvent}. A priority of 0 is the
	 * highest priority. Negative priorities will have their sign removed.
	 *
	 * @param priority
	 *            the new priority
	 */
	public synchronized void setPriority(int priority) {
		priority = Math.abs(priority);
		if (this.priority != priority) {
			this.priority = priority;
			// update object in MongoDB
			DB db = ControlServer.getInstance().db;
			DBCollection experiments = db
					.getCollection(DBCollections.EXPERIMENTS);
			BasicDBObject pushObj = new BasicDBObject("$set",
					new BasicDBObject("priority", priority));
			experiments.update(new BasicDBObject("_id", getName()), pushObj);
			this.setChanged();
			LOG.debug("priority changed to %d", priority);
			this.notifyObservers(new PriorityChangedEvent(priority));
		}
	}

	/**
	 * Returns the date and time when this experiment was created.
	 *
	 * @return the date and time when this experiment was created
	 */
	public Date getCreatedDate() {
		return createdDate;
	}

	/**
	 * Returns the date and time when this experiment was started (switched into
	 * {@link Experiment.STATE_ACTIVE}).
	 *
	 * @return the date and time when this experiment was started
	 */
	public Date getStartedDate() {
		return startedDate;
	}

	/**
	 * Returns the date and time when this experiment was finished (switched
	 * into one of the states in {@link Experiment.FINISH_STATES}.
	 *
	 * @return the date and time when this experiment was finished
	 */
	public Date getFinishedDate() {
		return finishedDate;
	}

	/**
	 * Returns the state of the Experiment. See {@code STATE_*} constants.
	 *
	 * @return the current state of the Experiment.
	 */
	public synchronized String getState() {
		return this.state;
	}

	/**
	 * Set the state of the Experiment. If the state changes any observers of
	 * this Experiment are notified with the new state. See {@code STATE_*}
	 * constants.
	 *
	 * There are currently no checks made if it is legal to actually change
	 * between the two states, so know what you are doing.
	 *
	 * @param state
	 *            the new state
	 */
	private synchronized void setState(String state) {
		if (!this.state.equalsIgnoreCase(state)) {
			String previousState = this.state;
			this.state = state;
			this.setChanged();
			// update object in MongoDB
			DB db = ControlServer.getInstance().db;
			DBCollection experiments = db
					.getCollection(DBCollections.EXPERIMENTS);
			BasicDBObject pushObj = new BasicDBObject("$set",
					new BasicDBObject("state", state));
			experiments.update(new BasicDBObject("_id", getName()), pushObj);
			LOG.debug("state changed to %s", state);
			this.notifyObservers(new StateChangedEvent(state, previousState));
		}
	}

	/**
	 * Puts this Experiment into {@link #STATE_ACTIVE}, starting the Experiment.
	 * This method is used by the {@link Scheduler} to start the Experiment, but
	 * could also be used to circumvene the scheduling done by the Scheduler
	 * (nevertheless the Experiment has to be added to the Scheduler).
	 */
	public synchronized void start() {
		this.setState(STATE_ACTIVE);
		if (this.jobs.isEmpty()) {
			// if this is an empty Experiment no jobs will be started and the
			// state would never move on if we didn't set it to SUCCESS here
			this.setState(STATE_SUCCESS);
		}
	}

	/**
	 * Returns the next job to be started from this Experiment. This does not
	 * automatically start that job.
	 *
	 * @return the next job to start or null if there aren't any jobs left
	 */
	public synchronized CSJob getNextJob() {
		if (this.unstartedJobs.isEmpty()) {
			return null;
		}
		return this.unstartedJobs.get(0);
	}

	/**
	 * Assigns the given worker to this Experiment. If any workers have been
	 * assigned to an Experiment the {@link Scheduler} will only use these
	 * workers for the Experiment. If no workers have been assigned, the
	 * Scheduler is free to use any worker.<br>
	 * The assign operation is only performed if this particular worker had not
	 * been assigned already: No duplicate entries.
	 *
	 * @param worker
	 *            the worker to assign as specified by its WorkerDescriptor
	 */
	public synchronized void assignWorker(WorkerDescriptor worker) {
		if (!this.assignedWorkers.contains(worker)) {
			this.assignedWorkers.add(worker);
			// update object in MongoDB
			DB db = ControlServer.getInstance().db;
			DBCollection experiments = db
					.getCollection(DBCollections.EXPERIMENTS);
			BasicDBObject pushObj = new BasicDBObject("$push",
					new BasicDBObject("assignedWorkers", worker.getName()));
			experiments.update(new BasicDBObject("_id", getName()), pushObj);
		}
	}

	/**
	 * Unassigns the given worker from this Experiment. If no workers remain
	 * assigned to this Experiment the {@link Scheduler} is now free to use any
	 * worker. If workers remain assigned to this Experiment only these
	 * remaining workers may be used by the Scheduler to execute the Experiment.
	 *
	 * @param worker
	 *            the worker to unassign as specified by its WorkerDescriptor
	 */
	public synchronized void unassignWorker(WorkerDescriptor worker) {
		this.assignedWorkers.remove(worker);
		// update object in MongoDB
		DB db = ControlServer.getInstance().db;
		DBCollection experiments = db.getCollection(DBCollections.EXPERIMENTS);
		BasicDBObject pushObj = new BasicDBObject("$pull", new BasicDBObject(
				"assignedWorkers", worker.getName()));
		experiments.update(new BasicDBObject("_id", getName()), pushObj);
	}

	/**
	 * Returns all {@link WorkerDescriptor} of the workers assigned to this
	 * Experiment.
	 *
	 * You may modify individual members of the returned list, but not the list
	 * itself.
	 *
	 * @return all workers assigned to this Experiment
	 */
	public synchronized List<WorkerDescriptor> getAssignedWorkers() {
		return this.assignedWorkers;
	}

	/**
	 * Returns all jobs that haven't been scheduled.
	 *
	 * @return list of unscheduled jobs
	 */
	public synchronized List<CSJob> getUnstartedJobs() {
		return this.unstartedJobs;
	}

	/**
	 * Returns an assigned {@link WorkerDescriptor} that is not busy, or null if
	 * all assigned workers are busy. If there are no assigned workers null is
	 * returned as well.
	 *
	 * @return a unemployed assigned worker or null
	 */
	public synchronized WorkerDescriptor getUnemployedAssignedWorker() {
		for (WorkerDescriptor w : this.assignedWorkers) {
			if (w.getState()
					.equalsIgnoreCase(WorkerDescriptor.STATE_UNEMPLOYED)) {
				return w;
			}
		}
		return null;
	}

	/**
	 * Returns the measured data for all jobs of this experiment.
	 *
	 * @return the {@link MeasuredData} object for all jobs of this experiment
	 */
	public MeasuredData getMeasuredData() {
		BasicDBObject query = new BasicDBObject("parentExperiment",
				this.getName());
		return new MeasuredData(query);
	}

	/**
	 * Returns all {@link Experiment}s known.
	 *
	 * @return all experiments
	 */
	public static Collection<Experiment> getExperiments() {
		synchronized (experiments) {
			return experiments.values();
		}
	}

	/**
	 * Returns the Experiment with given name or null if no such
	 * {@link Experiment} exists.
	 *
	 * @param name
	 *            name of the Experiment to return
	 * @return the Experiment or null
	 */
	public synchronized static Experiment getExperiment(String name) {
		synchronized (experiments) {
			return experiments.get(name);
		}
	}

	/**
	 * Add n copies of a {@link CSJob} to this Experiment and return them. All
	 * the jobs in an Experiment must execute the same algorithm (in name and
	 * version) with the same parameters but may differ in the supplied graph
	 * instance. This called the Experiment Contract and an
	 * {@link ControlServerException} is thrown if it is violated. Each copy is
	 * assigned a unique job id.
	 *
	 * Jobs may only be added and removed while the Experiment is in
	 * {@link #STATE_NEW}. If the Experiment is in any other state a
	 * {@link ControlServerException} is thrown.
	 *
	 * @param job
	 *            the job of which n copies are to be added
	 * @param n
	 *            the number of copies to add.
	 * @returns a list of all job copies added
	 * @throws ControlServerException
	 *             if the Experiment Contract is violated or the Experiment is
	 *             not in {@link #STATE_NEW}
	 */
	public synchronized List<CSJob> addJobs(CSJob job, int n)
			throws ControlServerException {
		// only STATE_NEW is allowed
		if (!this.getState().equalsIgnoreCase(STATE_NEW)) {
			throw new ControlServerException("You may only add jobs while the "
					+ "Experiment is in the NEW state.");
		}
		// check Experiment Contract
		if (!this.jobs.isEmpty()) {
			CSJob reference = this.jobs.get(0);
			job.obeysExperimentContract(reference);
		}

		// get MongoDB collection
		DB db = ControlServer.getInstance().db;
		DBCollection jobs = db.getCollection(DBCollections.JOBS);

		// add n copies
		ArrayList<CSJob> copies = new ArrayList<CSJob>(n);
		for (int i = 0; i < n; i++) {
			CSJob copy = job.createCopy(this.jobIDCounter);
			this.jobIDCounter++;
			copy.addObserver(this.jobObserver);
			this.jobs.add(copy);
			this.unstartedJobs.add(copy);
			copies.add(copy);

			// add job to jobs collection
			BasicDBObject db_job = new BasicDBObject();
			db_job.put("_id", copy.getID());
			db_job.put("parentExperiment", getName());
			db_job.put("state", getState());
			db_job.put("worker", copy.getWorker());
			db_job.put("log", new BasicDBList());
			db_job.put("measuredData", new BasicDBObject());
			db_job.put("executedDatetime", null);
			db_job.put("finishedDatetime", null);
			db_job.put("metadata", copy.getMetadata());
			jobs.insert(db_job);
		}
		LOG.debug("added %d jobs", n);
		return copies;
	}

	/**
	 * Remove a job from this experiment. More precisely it removes a job from
	 * this experiment that has the same ID as the given job. Or in other words
	 * previously added n copies won't be removed by one single call - you have
	 * to call this once for each copy.
	 *
	 * Jobs may only be added and removed while the Experiment is in
	 * {@link #STATE_NEW}. If the Experiment is in any other state a
	 * {@link ControlServerException} is thrown.
	 *
	 * @param job
	 *            the job to removed
	 * @throws ControlServerException
	 *             if the Experiment is not in {@link #STATE_NEW}
	 */
	public synchronized void removeJob(CSJob job) throws ControlServerException {
		// only STATE_NEW is allowed
		if (!this.getState().equalsIgnoreCase(STATE_NEW)) {
			throw new ControlServerException("You may only remove jobs while "
					+ "the Experiment is in the NEW state.");
		}
		// remove job
		this.jobs.remove(job);
		this.unstartedJobs.add(job);
		job.deleteObserver(this.jobObserver);
	}

	/**
	 * Returns all jobs that make up this Experiment.
	 *
	 * You may modify individual members of the returned list, but not the list
	 * itself.
	 *
	 * @return all jobs of this Experiment
	 */
	public synchronized List<CSJob> getJobs() {
		return this.jobs;
	}

	private void loadJobs() {
		// lazily load jobs
		DB db = Config.getDBInstance();
		DBCollection jobs = db.getCollection(DBCollections.JOBS);
		DBCursor experimentJobs = jobs.find(new BasicDBObject(
				"parentExperiment", this.getName()));
		for (DBObject dbo : experimentJobs) {
			CSJob job = new CSJob(dbo, this);
			job.addObserver(this.jobObserver);
			this.jobs.add(job);
			if (job.getState().isFinal()) {
				this.finishedJobs.add(job);
			} else if (job.getState().equals(Job.State.NEW)) {
				this.unstartedJobs.add(job);
			} else {
				this.runningJobs.add(job);
			}
		}
	}

	/**
	 * Returns the number of jobs in this Experiment.
	 *
	 * @return the number of jobs
	 */
	public synchronized int getJobCount() {
		return this.jobs.size();
	}

	/**
	 * Returns the number of jobs currently running.
	 *
	 * @return the number of jobs currently running
	 */
	public synchronized int getRunningCount() {
		return this.runningJobs.size();
	}

	/**
	 * Returns the number of jobs that were initialized or started.
	 *
	 * @return the number of jobs started
	 */
	public synchronized int getInitializedCount() {
		return this.initialized_count;
	}

	/**
	 * Returns the number of jobs that reached {@link Job#STATE_RUNNING}.
	 *
	 * @return the number of jobs started
	 */
	public synchronized int getProcessedCount() {
		return this.processed_count;
	}

	/**
	 * The number of jobs that were successful.
	 *
	 * @return the number of successful jobs
	 */
	public synchronized int getSuccessCount() {
		return this.success_count;
	}

	/**
	 * The number of jobs that have failed
	 *
	 * @return number of failed jobs
	 */
	public synchronized int getFailedCount() {
		return this.failed_count;
	}

	/**
	 * The number of jobs that were cancelled by the user
	 *
	 * @return the number of cancelled jobs
	 */
	public synchronized int getCancelledCount() {
		return this.cancelled_count;
	}

	/**
	 * The number of jobs that were aborted by a timer
	 *
	 * @return the number of aborted jobs
	 */
	public synchronized int getAbortedCount() {
		return this.aborted_count;
	}

	/**
	 * Cancel the Experiment. Note that the Experiment won't be cancelled
	 * immediately, but enters {@link #STATE_CANCELLING}.
	 * {@link #STATE_CANCELLED} is only reached when all jobs of this Experiment
	 * have been cancelled.
	 */
	public synchronized void cancel() {
		this.setState(STATE_CANCELLING);
		for (CSJob job : this.jobs) {
			job.cancel();
		}
		this.setState(STATE_CANCELLED);
	}

	/**
	 * Returns true if an Experiment with the given name already exists.
	 *
	 * @param name
	 *            the name to check
	 * @return true if an Experiment with that name exists
	 */
	public synchronized static boolean exists(String name) {
		DB db = ControlServer.getInstance().db;
		DBCollection coll = db.getCollection(DBCollections.EXPERIMENTS);
		long count = coll.getCount(new BasicDBObject("_id", name));
		return count == 1;
	}

	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Experiment)) {
			return false;
		}
		Experiment other = (Experiment) obj;
		return this.getName().equals(other.getName());
	}

	@Override
	public String toString() {
		return "Experiment-" + this.name;
	}

	/**
	 * Observer for all jobs of an Experiment. Updates the state of the
	 * Experiment.
	 */
	private class JobObserver implements Observer, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = -2408214324660044084L;

		@Override
		public void update(Observable o, Object arg) {
			synchronized (Experiment.this) {
				CSJob job = (CSJob) o;
				JobState state = (JobState) arg;
				// react to changing job state
				if (state instanceof Job.State) {
					switch ((Job.State) state) {
					case ABORTED:
						Experiment.this.finishedJobs.add(job);
						Experiment.this.runningJobs.remove(job);
						Experiment.this.aborted_count++;
						break;
					case CANCELLED:
						Experiment.this.finishedJobs.add(job);
						Experiment.this.runningJobs.remove(job);
						Experiment.this.cancelled_count++;
						break;
					case FAILED:
						Experiment.this.finishedJobs.add(job);
						Experiment.this.runningJobs.remove(job);
						Experiment.this.failed_count++;
						break;
					case SUCCESS:
						Experiment.this.finishedJobs.add(job);
						Experiment.this.runningJobs.remove(job);
						Experiment.this.success_count++;
						break;
					case INITIALIZING:
						Experiment.this.initialized_count++;
						break;
					case SCHEDULED:
						Experiment.this.scheduled_count++;
						Experiment.this.unstartedJobs.remove(job);
						break;
					case RUNNING:
						Experiment.this.runningJobs.add(job);
						Experiment.this.processed_count++;
						break;
					default:
						break;
					}
				}

				// check if this Experiment is finished
				int jobCount = Experiment.this.getJobCount();
				int significantJobCount = jobCount
						- Experiment.this.cancelled_count
						- Experiment.this.aborted_count;
				if (Experiment.this.finishedJobs.size() == jobCount) {
					// check how successful the Experiment was
					if (Experiment.this.getState().equalsIgnoreCase(
							STATE_CANCELLING)
							|| Experiment.this.cancelled_count == jobCount) {
						// the Experiment was cancelled
						Experiment.this.setState(STATE_CANCELLED);
					} else if (Experiment.this.failed_count == significantJobCount) {
						// all jobs failed, were cancelled or aborted
						Experiment.this.setState(STATE_FAILED);
					} else if (Experiment.this.success_count == significantJobCount) {
						// all jobs either succeeded, were cancelled or aborted
						Experiment.this.setState(STATE_SUCCESS);
					} else {
						// at least one job failed and at least one job
						// succeeded,
						// some may have been aborted or cancelled
						Experiment.this.setState(STATE_PARTIAL_SUCCESS);
					}
					// return here so the running count isn't checked - because
					// that is always 0 when this block is entered
					return;
				}

				// don't change the state in PAUSING or PAUSED
				if (Experiment.this.state.equalsIgnoreCase(STATE_PAUSING)
						|| Experiment.this.state.equalsIgnoreCase(STATE_PAUSED)) {
					return;
				}

				// check on the number of jobs currently running
				if ((Experiment.this.getRunningCount() == 0)
						&& (Experiment.this.initialized_count == 0)
						&& (Experiment.this.scheduled_count == 0)) {
					Experiment.this.setState(STATE_WAITING);
				} else {
					Experiment.this.setState(STATE_ACTIVE);
				}
			}
		}

	}

	/**
	 * An event indicating that the state of the Experiment has changed.
	 */
	public class StateChangedEvent implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -8444220274744664723L;
		public String state;
		public String previousState;

		public StateChangedEvent(String state, String previousState) {
			super();
			this.state = state;
			this.previousState = previousState;
		}
	}

	/**
	 * An event indicating that the priority of the Experiment has changed.
	 */
	public class PriorityChangedEvent implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 2915420059647328070L;
		public int priority;

		public PriorityChangedEvent(int priority) {
			super();
			this.priority = priority;
		}
	}

	/**
	 * Sets the HashMap containing the algorithm and its parameters.
	 *
	 * @param hashmap
	 */
	public void setMap(Map<String, Object> map) {
		this.map = map;
		DB db = ControlServer.getInstance().db;
		DBCollection exps = db.getCollection(DBCollections.EXPERIMENTS);
		finishedDate = new Date();
		BasicDBObject pushObj = new BasicDBObject("$set", new BasicDBObject(
				"map", this.map));
		exps.update(new BasicDBObject("_id", getName()), pushObj);
	}

	public Map<String, Object> getHashmap() {
		return this.map;
	}

	/**
	 * Returns the original experiment or null.
	 *
	 * @return The original experiment or null if this is the original
	 *         experiment
	 */
	public Experiment getRepeatOf() {
		return this.repeatOf;
	}

	/**
	 * Creates a copy of this experiment but resets all values which are
	 * necessary for a repeat.
	 *
	 * @return A reseted copy of this experiment
	 */
	public Experiment repeat() {
		try {
			return new Experiment(this);
		} catch (ControlServerException e) {
			e.printStackTrace();
		}
		return null;
	}

	void markReady() {
		if (state.equalsIgnoreCase(STATE_NEW)
				|| state.equalsIgnoreCase(STATE_READY)) {
			state = STATE_READY;
		} else {
			throw new RuntimeException("Cannot call markReady() on the state: "
					+ state);
		}
	}
}
