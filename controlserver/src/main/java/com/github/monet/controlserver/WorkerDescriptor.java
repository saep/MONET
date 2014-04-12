package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import com.github.monet.common.DBCollections;
import com.github.monet.common.logging.LogEvent;
import com.github.monet.common.logging.LoggingListener;
import com.github.monet.common.logging.LoggingPublisher;
import com.github.monet.worker.Communicator;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * Contains all information about a worker such as {@code ip}, {@code port},
 * {@code cpu}, {@code ram}, {@code name}, ...<br>
 *
 * Also maintains the employment status of the worker and notifies any observers
 * of the new employment status when it changes.
 *
 * @author Marco Kuhnke, Andreas Pauly, Max Günther
 */
public class WorkerDescriptor extends Observable implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -8651224040535264828L;
	/**
	 * The IP of the worker.
	 */
	public String ip;
	/**
	 * The CPU of the worker.
	 */
	public String cpu;
	/**
	 * The RAM of the worker in MiB.
	 */
	public String ram;
	/**
	 * The name of the worker.
	 */
	private String name;
	/**
	 * A worker may be employed or unemployed.
	 */
	private String state;

	/**
	 * The job currently being executed by the worker, or null if none is
	 * running.
	 */
	private CSJob job;

	/**
	 * Log of the Worker handling the serialization of log messages.
	 */
	private WorkerLog workerLog;

	/**
	 * Attribute to determine whether the worker is still available.
	 * <p>
	 * A value of 0 worker is available.
	 */
	private long attendanceDeadline;

	/**
	 * Map of all workers.
	 */
	private static Map<String, WorkerDescriptor> workers;

	/**
	 * A LoggingPublisher for all logging messages received from the worker.
	 */
	private LoggingPublisher logPublisher = new LoggingPublisher();

	public static final String STATE_UNEMPLOYED = "unemployed";
	public static final String STATE_EMPLOYED = "employed";
	/**
	 * State of the worker when no connection could be established, but the
	 * MONET still remembers the worker.
	 */
	public static final String STATE_UNAVAILABLE = "unavailable";

	static {
		workers = new HashMap<>();
	}

	/**
	 * Base constructor of the worker.
	 */
	private WorkerDescriptor() {
		this.attendanceDeadline = 0;
		this.state = "";
		this.setState(STATE_UNEMPLOYED);

		// logging
		this.logPublisher.addLoggingListener(new LoggingListener() {
			/**
			 *
			 */
			private static final long serialVersionUID = 8324852091378494084L;

			@Override
			public void logEvent(LogEvent event) {
				System.out.println(event.toString());
			}
		});
		this.workerLog = new WorkerLog(this.name);
		this.logPublisher.addLoggingListener(this.workerLog);
	}

	/**
	 * This constructor creates a {@link WorkerDescriptor} from a DBObject.
	 *
	 * @param dbo
	 *            a MongoDB object of a Worker
	 */
	private WorkerDescriptor(DBObject dbo) {
		this();
		name = (String) dbo.get("_id");
		ip = (String) dbo.get("ip");
		cpu = (String) dbo.get("cpu");
		ram = (String) dbo.get("ram");
		this.setState(STATE_UNAVAILABLE);
		workers.put(name, this);
	}

	/**
	 * Constructor that creates a new worker. It's state is initially set to
	 * <i>unemployed</i>.
	 *
	 * @param ip
	 *            the last known IP of the Worker
	 * @param port
	 *            the last known port of the Worker
	 * @param cpu
	 *            the CPU of the Worker
	 * @param ram
	 *            the RAM in kb of the Worker
	 * @param name
	 *            the unique name of the Worker
	 * @throws ControlServerException
	 *             if a worker with that name already exists
	 */
	public WorkerDescriptor(String ip, String cpu, String ram,
			String name) throws ControlServerException {
		this();
		this.ip = ip;
		this.cpu = cpu;
		this.ram = ram;
		this.name = name;

		// put into map of all workers
		if (workers.containsKey(name)) {
			throw new ControlServerException(String.format(
					"Worker %s already exists.", name));
		}
		workers.put(name, this);

		// write to MongoDB
		DB db = ControlServer.getInstance().db;
		DBCollection coll = db.getCollection(DBCollections.WORKERS);
		BasicDBObject doc = new BasicDBObject();
		doc.put("_id", this.name);
		doc.put("ip", this.ip);
		doc.put("cpu", this.cpu);
		doc.put("ram", this.ram);
		doc.put("state", this.state);
		doc.put("log", new BasicDBList());
		coll.save(doc);
	}

	/**
	 * Loads any workers from the Database and tries to connect to them.
	 */
	public static void loadWorkersFromDB() {
		// retrieve workers from DB
		DB db = ControlServer.getInstance().db;
		DBCollection experiments = db.getCollection(DBCollections.WORKERS);
		for (DBObject dbo : experiments.find()) {
			/* The constructor has side-effects. */
			new WorkerDescriptor(dbo);
		}
	}

	/**
	 * Returns all workers that are known.
	 *
	 * @return list of all workers
	 */
	public static Map<String, WorkerDescriptor> getWorkers() {
		return Collections.unmodifiableMap(workers);
	}

	/**
	 * Set the state of the worker. If the state has changed all observers are
	 * notified with the new state. If the state is set to
	 * {@link #STATE_UNEMPLOYED} the currently executed job is set to null.
	 *
	 * @param state
	 *            the new state
	 */
	public void setState(String state) {
		if (!this.state.equals(state)) {
			if (state.equals(STATE_UNEMPLOYED)) {
				this.job = null;
			}
			// update object in MongoDB
			DB db = ControlServer.getInstance().db;
			DBCollection experiments = db.getCollection(DBCollections.WORKERS);
			BasicDBObject pushObj = new BasicDBObject("$set",
					new BasicDBObject("state", state));
			experiments.update(new BasicDBObject("_id", getName()), pushObj);
			// update own attributes
			this.state = state;
			this.setChanged();
			this.notifyObservers(state);
		}
	}

	/**
	 * method used to request an attendance verification for given worker in the
	 * time given by static attribute attendanceTimer.
	 */
	public void checkAttendance(WorkerClientThread wct) {
		attendanceDeadline = System.currentTimeMillis() + Communicator.PING_INTERVAL;
		wct.doWait();
		wct.sendMessage(Communicator.NET_PING);
		wct.sendEndingMessage();

	}

	/**
	 * Indicator whether the worker is connected to the controlserver.
	 * <p>
	 *
	 * @return true if the worker is attended.
	 */
	public boolean isResponding() {
		return attendanceDeadline == 0;
	}

	/**
	 * sets the worker to attended state. This method should only be used by the
	 * controlserver after it got an attendance verification.
	 *
	 * @param responsive
	 *            set the responsive state of the worker
	 */
	public void setResponsive(boolean responsive) {
		if (responsive) {
			this.attendanceDeadline = 0;
		} else {
			attendanceDeadline = System.currentTimeMillis()
					+ Communicator.PING_RESPONSE_TIME;
		}
	}

	/**
	 * checks whether a worker's last attendance check timed out or not
	 *
	 * @return true if last attendance check failed
	 */
	public boolean isTimedOut() {
		return !isResponding() && (this.attendanceDeadline - System.currentTimeMillis()) < 0;
	}

	/**
	 * Returns the state of the worker. This may be one of
	 * {@link #STATE_EMPLOYED} and {@link #STATE_UNEMPLOYED}.
	 *
	 * @return the state of the worker
	 */
	public String getState() {
		return this.state;
	}

	/**
	 * Get the job currently being executed, or null if no job is running on the
	 * worker.
	 *
	 * @return the current job or null
	 */
	public CSJob getJob() {
		return this.job;
	}

	/**
	 * Set the job currently running. If the given job is null, the worker is
	 * put into {@link #STATE_UNEMPLOYED} otherwise it is put into
	 * {@link #STATE_EMPLOYED}.
	 *
	 * @param job
	 *            the job running or null if none is running
	 */
	public void setJob(CSJob job) {
		this.job = job;
		if (job == null) {
			this.setState(STATE_UNEMPLOYED);
		} else {
			this.setState(STATE_EMPLOYED);
		}
	}

	/**
	 * Returns the name of Worker. This name has to be unique and is generated
	 * from the computer name that the worker is running on.
	 *
	 * @return the name of the worker
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get the {@link LoggingPublisher} for the workers log. Note that this
	 * isn't the log of the job running on the worker.
	 *
	 * @return the LoggingPublisher
	 */
	public LoggingPublisher getLoggingPublisher() {
		return this.logPublisher;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WorkerDescriptor)) {
			return false;
		}
		WorkerDescriptor desc = (WorkerDescriptor) obj;
		return desc.name.equals(this.name);
	}

	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	@Override
	public String toString() {
		return "Worker-" + getName();
	}

	/**
	 * Returns the {@link WorkerLog} for this worker.
	 *
	 * @return Corresponding {@link WorkerLog}
	 */
	public WorkerLog getLog() {
		return this.workerLog;
	}

}
