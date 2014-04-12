package com.github.monet.worker;

import java.rmi.activation.Activator;
import java.util.Observable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.osgi.framework.BundleException;

import com.github.monet.interfaces.Meter;
import com.mongodb.DB;

/**
 * Facilitates experiments and persists the entire runtime of the worker.
 *
 * <h2>Experiment life cycle</h2> The method {@link #isBusy()} returns false if
 * (and only if) the actualJob is null. This is sufficient because of the
 * behavior of the {@link JobThread} class which calls
 * {@link #experimentFinished()} at the end of its run() method: The actualJob
 * automatically gets reset to null as soon as an experiment finished
 * irrespective of its success, result, whatever...
 *
 * <h2>Meter</h2> The Experimentor stores the {@link Meter} object created and
 * set by the actual {@link JobThread}. The instance is provided by
 * {@link #getMeter()}.
 *
 * @author Marco Kuhnke
 */
public class Experimentor extends Observable {
	private final static Logger LOG = LogManager
			.getFormatterLogger(Experimentor.class);
	private ServiceDirectory serviceDirectory = null;
	private DB db = null;
	private WorkerJob actualJob = null;
	private Thread actualThread = null;
	public final static String JOB_STARTED = "job started";
	public final static String JOB_FINISHED = "job finished";

	/**
	 * Constructor called by the {@link Activator}.
	 *
	 * @param serviceDirectory
	 * @param db
	 *            Database (MongoDB)
	 */
	public Experimentor(ServiceDirectory serviceDirectory, DB db) {
		this.serviceDirectory = serviceDirectory;
		this.db = db;
	}

	/**
	 * Sets the actual job and gets the algorithm service, then starts the job
	 * thread.
	 *
	 * @param job
	 */
	public void startJob(WorkerJob job) throws ServiceNotFoundException,
			BundleException {
		LOG.info("starting job %s", job.getID());
		this.actualJob = job;
		this.actualJob.setState(Job.State.INITIALIZING);
		// start the job thread
		this.actualThread = new Thread(new JobThread(this, this.db,
				this.serviceDirectory));
		String jobid = job.getID();
		String threadName = String.format("JobThread-%s", jobid);
		this.actualThread.setName(threadName);
		this.setChanged();
		this.notifyObservers(JOB_STARTED);
		this.actualThread.start();
	}

	/**
	 * Method used to kill the actual Job.
	 *
	 * @Author David Mezlaf
	 */
	public void killJob() {
		if (this.isBusy()) {
			if (this.actualThread != null && !this.actualJob.getState().isFinal()) {
				this.actualJob.setState(Job.State.CANCELLING);
				actualThread.stop(new KillJobException());
			}
		}
	}

	/**
	 * Provides the Experimentors active Job (may be null).
	 *
	 * @return the actual job
	 */
	public WorkerJob getActiveJob() {
		return this.actualJob;
	}

	/**
	 * Whether or not the Experimentor is busy.
	 *
	 * @return true if actualJob is not null, else false
	 */
	public boolean isBusy() {
		return (this.actualJob != null);
	}

	/**
	 * Returns the instance of the ServiceDirectory.
	 *
	 * @return the instance of the ServiceDirectory
	 */
	ServiceDirectory getServiceDirectory() {
		return this.serviceDirectory;
	}

	/**
	 * Gets called by JobThread at the end of its run() method. Resets the
	 * actualJob and notifies the observers (Communicator).
	 *
	 * @param finalState
	 *            the state to put the current (though ending) job into
	 */
	void experimentFinished(JobState finalState) {
		LOG.info("job %s finished", this.actualJob.getID());
		// clear all references associated with the experiment
		Job job = this.actualJob;
		this.actualJob = null;
		this.actualThread = null;
		// reset the service directory
		this.serviceDirectory.stopAllBundles();
		job.setState(finalState);
		// notify all observers
		this.setChanged();
		this.notifyObservers(JOB_FINISHED);
	}
}
