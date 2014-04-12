package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.worker.Job;

/**
 * Assigns {@link Experiment}s to Workers using a priority queue. This also
 * means that the scheduler maintains all workers and experiments of MONet.
 *
 * The method {@link #schedule()} provides the next {@code CSJob} to process.
 *
 * This class is observable and notifies its observers with
 * {@link #JobStartedEvent}s.
 *
 * @author Marco Kuhnke, Andreas Pauly, Max Günther
 */
public class Scheduler extends Observable implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -7710669960699177721L;
	/**
	 * A prioritized queue of {@link Experiment}s. Experiments in this queue,
	 * have not been started, nor are they in {@link #finishedExperiments} or
	 * {@link #activeExperiments}.
	 */
	private PriorityQueue<Experiment> queue;
	/**
	 * A map name -> experiment of all known {@link Experiment}s.
	 */
	private Map<String, Experiment> experiments;
	/**
	 * A map name -> experiment of all {@link Experiment}s that are currently
	 * paused.
	 */
	private Map<String, Experiment> pausedExperiments;
	/**
	 * A linked list of Experiments whose jobs are currently being executed. The
	 * first elements in the list are Experiments that might have jobs still
	 * open, while the other elements do not. If one or more Experiments have
	 * been unpaused they are inserted just before the last Experiments that has
	 * no open jobs left, meaning that jobs of unpaused Experiments are executed
	 * in the order they were unpaused but never before any job of a currently
	 * running Experiment.
	 */
	private LinkedList<Experiment> activeExperiments;
	/**
	 * A list of {@link Experiment}s that are done, regardless of success.
	 */
	private Set<Experiment> finishedExperiments;

	/**
	 * A map name -> worker of all {@link WorkerDescriptor}s known.
	 */
	private Map<String, WorkerDescriptor> registeredWorkers;
	/**
	 * A map name -> worker of all {@link WorkerDescriptor} that are not
	 * executing any job.
	 */
	private Map<String, WorkerDescriptor> unemployedWorkers;
	/**
	 * A map worker -> experiment of all {@link Experiment} that are waiting for
	 * a specific {@link WorkerDescriptor} to be unemployed.
	 */
	private Map<WorkerDescriptor, Experiment> waitingForWorkers;
	/**
	 * Observes all {@link Experiment}s and reacts to changes in priority and
	 * and state.
	 */
	private ExperimentObserver experimentObserver;
	/**
	 * Observes all {@link WorkerDescriptor}s and reacts to changes in states.
	 */
	private WorkerObserver workerObserver;

	/**
	 * The logger for this Scheduler.
	 */
	private static Logger log = LogManager.getFormatterLogger(Scheduler.class);

	private static class ExperimentComparator implements
			Comparator<Experiment>, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 4834848509012504775L;

		@Override
		public int compare(Experiment exp1, Experiment exp2) {
			return Integer.compare(exp1.getPriority(), exp2.getPriority());
		}

	}

	/**
	 * Constructor.
	 */
	public Scheduler() {
		this.queue = new PriorityQueue<Experiment>(10,
				new ExperimentComparator());
		this.registeredWorkers = new HashMap<String, WorkerDescriptor>();
		this.unemployedWorkers = new HashMap<String, WorkerDescriptor>();
		this.waitingForWorkers = new HashMap<>();
		this.finishedExperiments = new HashSet<Experiment>();
		this.activeExperiments = new LinkedList<Experiment>();
		this.pausedExperiments = new HashMap<String, Experiment>();
		this.experiments = new HashMap<String, Experiment>();
		this.experimentObserver = new ExperimentObserver();
		this.workerObserver = new WorkerObserver();
		// FEATURE query the experiments collection to find all existing
		// experiments, max
		// FEATURE query the workers collection to find all existing workers,
		// max
	}

	/**
	 * Registers a worker with the Scheduler.
	 *
	 * @param worker
	 *            the worker to be registered
	 * @throws ControlServerException
	 *             thrown if a worker with that name is already registered
	 */
	public synchronized void registerWorker(WorkerDescriptor worker) {
		worker.addObserver(this.workerObserver);
		this.registeredWorkers.put(worker.getName(), worker);
		if (worker.getState().equalsIgnoreCase(WorkerDescriptor.STATE_UNEMPLOYED)) {
			this.unemployedWorkers.put(worker.getName(), worker);
		}
	}

	/**
	 * Unregisters a worker with the Scheduler.
	 *
	 * @param worker
	 *            the worker to be unregistered
	 */
	public synchronized void unregisterWorker(WorkerDescriptor worker) {
		// FIXME somehow let everybody else know that this worker is gone now
		worker.deleteObserver(this.workerObserver);
		this.registeredWorkers.remove(worker.getName());
	}

	/**
	 * Returns an unemployed worker or null if there are no unemployed workers
	 *
	 * @return an unemployed worker or null
	 */
	private synchronized WorkerDescriptor getUnemployedWorker() {
		if (this.unemployedWorkers.isEmpty()) {
			return null;
		} else {
			return this.unemployedWorkers.values().iterator().next();
		}
	}

	/**
	 * Returns a collection of all registered workers.
	 *
	 * @return The registered worker list
	 */
	public synchronized Collection<WorkerDescriptor> getWorkers() {
		return this.registeredWorkers.values();
	}

	/**
	 * Returns all workers that are not employed.
	 *
	 * @return a collection of workers that aren't employed
	 */
	public synchronized Collection<WorkerDescriptor> getUnemployedWorkers() {
		return this.unemployedWorkers.values();
	}

	/**
	 * Returns a map worker -> experiment of all {@link WorkerDescriptor} that
	 * are being waited for by {@link Experiment}s.
	 *
	 * @return a map of all workers waited on by Experiments
	 */
	public synchronized Map<WorkerDescriptor, Experiment> getWaitingForWorkers() {
		return this.waitingForWorkers;
	}

	/**
	 * Get the {@link Experiment} with the given name, or null if no such
	 * Experiment exists.
	 *
	 * @param name
	 *            the name of the Experiment in question
	 * @return the Experiment with the given name or null
	 */
	public synchronized Experiment getExperiment(String name) {
		return this.experiments.get(name);
	}

	/**
	 * Returns all {@link Experiment}s.
	 *
	 * @return all Experiments as a Collection
	 */
	public synchronized Collection<Experiment> getExperiments() {
		return this.experiments.values();
	}

	/**
	 * Queue an {@link Experiment}.
	 *
	 * @param experiment
	 *            the Experiment to add
	 * @throws ControlServerException
	 *             thrown if the experiment is not in
	 *             {@link Experiment#STATE_NEW} or
	 *             {@link Experiment#STATE_READY}
	 */
	public synchronized void addExperiment(Experiment experiment)
			throws ControlServerException {
		if (!experiment.getState().equalsIgnoreCase(Experiment.STATE_NEW)
				&& !experiment.getState().equalsIgnoreCase(
						Experiment.STATE_READY)) {
			throw new ControlServerException("experiment no in state new");
		}
		experiment.markReady();
		experiment.addObserver(this.experimentObserver);
		this.experiments.put(experiment.getName(), experiment);
		this.queue.add(experiment);
	}

	/**
	 * Returns the next {@link CSJob} to schedule from the added experiments. If
	 * there is no undone job left, it returns null.
	 *
	 * @return The next {@link CSJob} to process or null
	 */
	public synchronized CSJob schedule() {
		Experiment exp = null;
		CSJob job = null;
		WorkerDescriptor worker = null;

		if (this.unemployedWorkers.isEmpty()) {
			// this.log.trace("nothing is scheduled because all workers are busy");
			return null;
		} else if (this.activeExperiments.isEmpty()) {
			exp = null;
			job = null;
		} else {
			// go through unemployed workers to look if there is one that an
			// Experiment is waiting for
			boolean foundAssignedWorker = false;
			for (WorkerDescriptor wd : this.unemployedWorkers.values()) {
				if (this.waitingForWorkers.containsKey(wd)) {
					exp = this.waitingForWorkers.get(wd);
					job = exp.getNextJob();
					worker = wd;
					foundAssignedWorker = true;
					log.debug("found unemployed assigned worker %s for %s",
							wd.getName(), exp.getName());
					break;
				}
			}
			if (!foundAssignedWorker) {
				exp = this.activeExperiments.getFirst();
				if (exp.getAssignedWorkers().isEmpty()) {
					job = exp.getNextJob();
					// get an unemployed worker
					worker = getUnemployedWorker();
				}
			}
		}
		while (job == null) {
			exp = this.queue.peek();
			if (exp == null) {
				return null;
			} else {
				exp.start();
				for (WorkerDescriptor wd : exp.getAssignedWorkers()) {
					this.waitingForWorkers.put(wd, exp);
				}
				// Experiment is removed from the queue by the
				// experimentObserver
				return schedule(); // simply start over with the experiment that
				// has just been started
			}
		}

		// schedule that job
		log.debug("scheduling %s in %s to %s", job, exp, worker);
		job.setState(Job.State.SCHEDULED);
		job.setWorker(worker);
		worker.setJob(job); // also sets the worker to EMPLOYED
		JobStartedEvent event = new JobStartedEvent(exp, job);
		this.setChanged();
		this.notifyObservers(event);
		return job;
	}

	/**
	 * Returns the {@link WorkerDescriptor} which is specified by it's name.
	 *
	 * @param name
	 * @return specified worker or null if not found
	 */
	public synchronized WorkerDescriptor getWorkerDescriptor(String name) {
		return this.registeredWorkers.get(name);
	}

	/**
	 * This event is used to announce that the {@link ControlServer} has been
	 * asked to start a {@link CSJob} in an {@link Experiment}.
	 */
	public class JobStartedEvent implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 7647962364698157555L;
		public Experiment experiment;
		public CSJob job;

		public JobStartedEvent(Experiment experiment, CSJob job) {
			super();
			this.experiment = experiment;
			this.job = job;
		}
	}

	/**
	 * Observes all {@link Experiment}s known to the Scheduler.
	 */
	private class ExperimentObserver implements Observer, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = -1439372238090054500L;

		@Override
		public void update(Observable obs, Object obj) {
			synchronized (Scheduler.this) {
				Experiment exp = (Experiment) obs;
				if (obj instanceof Experiment.PriorityChangedEvent) {
					// add and remove the Experiment from the priority queue
					synchronized (Scheduler.this) {
						if (queue.contains(exp)) {
							queue.remove(exp);
							queue.add(exp);
							log.debug("Experiments %s changed priority to %d",
									exp.getName(), exp.getPriority());
						}
					}
				} else if (obj instanceof Experiment.StateChangedEvent) {
					synchronized (this) {
						if (exp.getState().equalsIgnoreCase(Experiment.STATE_ACTIVE)) {
							Experiment.StateChangedEvent event = (Experiment.StateChangedEvent) obj;
							if (event.previousState
									.equalsIgnoreCase(Experiment.STATE_PAUSED)) {
								pausedExperiments.remove(exp.getName());
								// insert after any currently running
								// Experiments that have jobs left to be done
								int i = 0;
								for (Experiment e : activeExperiments) {
									if (e.getNextJob() == null) {
										break;
									}
									i++;
								}
								activeExperiments.add(i, exp);
							} else {
								queue.remove(exp);
								activeExperiments.addFirst(exp);
							}
						} else if (exp.getState().equalsIgnoreCase(
								Experiment.STATE_CANCELLING)) {
							pausedExperiments.remove(exp.getName());
							activeExperiments.remove(exp.getName());
						} else if (Experiment.FINISH_STATES.contains(exp
								.getState())) {
							List<Experiment> oneExp = new ArrayList<>();
							oneExp.add(exp);
							waitingForWorkers.values().removeAll(oneExp);
							pausedExperiments.remove(exp.getName());
							activeExperiments.remove(exp.getName());
							finishedExperiments.add(exp);
						} else if (exp.getState().equalsIgnoreCase(
								Experiment.STATE_PAUSED)) {
							activeExperiments.remove(exp.getName());
							pausedExperiments.put(exp.getName(), exp);
						} else {
							// STATE_WAITING and STATE_PAUSING are ignored for
							// now
						}
					}
				}
			}
		}

	}

	/**
	 * Observes all {@link WorkerDescriptor}s known to the Scheduler and reacts
	 * to their state.
	 */
	private class WorkerObserver implements Observer, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 3898474384178344916L;

		@Override
		public void update(Observable obs, Object arg) {
			WorkerDescriptor worker = (WorkerDescriptor) obs;
			synchronized (Scheduler.this) {
				switch (worker.getState()) {
				case WorkerDescriptor.STATE_EMPLOYED:
					Scheduler.this.unemployedWorkers.remove(worker.getName());
					break;
				case WorkerDescriptor.STATE_UNEMPLOYED:
					Scheduler.this.unemployedWorkers.put(worker.getName(),
							worker);
					break;
				}
			}
		}
	}

}
