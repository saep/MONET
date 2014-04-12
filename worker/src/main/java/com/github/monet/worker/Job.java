package com.github.monet.worker;

import java.io.Serializable;
import java.util.Map;
import java.util.Observable;

import org.apache.logging.log4j.Logger;

import com.github.monet.worker.JobState.CustomState;

/**
 * This class contains the state and the meta informations of a job.
 *
 * <p>
 * <h2>Get parsed input graph:</h2>
 * To get the instance of the parsed input graph, use the method
 * {@link #getInputGraph()}.
 * </p>
 * <p>
 * <h2>Access parameters:</h2>
 * To access the parameters of the current Job object, use the method
 * {@link #getParameters()}.
 * </p>
 * <p>
 * <h2>Set the state:</h2>
 * To set the state of the current Job object, use the method
 * {@link #setState(String)} in combination with the static state strings
 * {@link #STATE_RUNNING}, {@link #STATE_PHASE1} and {@link #STATE_PHASE2} or
 * set a custom string, e.g. <i>'refreshing labels'</i>. It is not recommended
 * to use any of the other static state strings, because they are set by the
 * classes which handle the execution of the Job object.
 * </p>
 * <p>
 * <h2>Access meta informations:</h2>
 * To access the meta informations of the current Job object, use the methods
 * {@link #getAlgorithmDescriptor()}, {@link #getGraphDescriptor()},
 * {@link #getInputGraphPath()} and {@link #getParserDescriptor()}. Since the
 * algorithm gets a parsed instance of the graph, it should not be necessary to
 * read any of this meta informations at the point of executing the algorithm.
 * </p>
 * <p>
 * <h2>Note:</h2>
 * <ul>
 * <li>Developers of algorithm implementations shall not create new instances of
 * the Job class.</li>
 * <li>Each update of the state, which is applied through
 * {@link #setState(String)} calls the {@link #notifyAll()} method, so that the
 * developer of an algorithm implementation does not have to handle the
 * notification</li>
 * </ul>
 * </p>
 *
 */
public abstract class Job extends Observable implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -878510250225788811L;

	/**
	 * This enumeration describes the various states a job can be in.
	 */
	public enum State implements JobState {
		/**
		 * A job has been created.
		 */
		NEW,
		/**
		 * Indicates that a job is marked as scheduled and will be executed at
		 * some point in the future.
		 */
		SCHEDULED,
		/**
		 * Indicates that a worker is initializing the required packages.
		 */
		INITIALIZING,
		/**
		 * The graph for the job is currently parsed.
		 */
		PARSING,
		/**
		 * Indicator that a job is currently running.
		 */
		RUNNING,
		/**
		 * Indicates that the worker is calculating some metrics for the job.
		 * You can also tentatively say that the job was successful.
		 */
		CALCULATING_METRICS,
		/**
		 * Indicator that the job has been run successfully.
		 */
		SUCCESS,
		/**
		 * Indicator that the job has been aborted for an unknown reason.
		 */
		ABORTED,
		/**
		 * Indicator that the job has been cancelled by a user.
		 */
		CANCELLED,
		/**
		 * Indicator that a job has failed.
		 */
		FAILED,
		/**
		 * Indicator that the job is currently cleaning up resources because it
		 * was cancelled by a user.
		 */
		CANCELLING;

		/**
		 * @return true if this state is considered a final state
		 */
		public boolean isFinal() {
			switch (this) {
			case ABORTED:
			case CANCELLED:
			case FAILED:
			case SUCCESS:
				return true;
			default:
				return false;
			}
		}

		@Override
		public boolean isStateTransitionAllowed(JobState newState) {
			if (this.isFinal()) {
				return false;
			} else if (this.equals(newState)) {
				return true;
			} else if (!(newState instanceof State)) {
				return this == RUNNING;
			}
			switch ((State) newState) {
			case ABORTED:
			case FAILED:
			case CANCELLING:
				return true;
			default:
				switch (this) {
				case CALCULATING_METRICS:
					return newState.isFinal();
				case CANCELLING:
					return newState == CANCELLED;
				case INITIALIZING:
					return newState == PARSING;
				case NEW:
					return (newState == SCHEDULED) || (newState == CANCELLED);
				case PARSING:
					return newState == RUNNING;
				case RUNNING:
					return newState.isFinal()
							|| newState == CALCULATING_METRICS;
				case SCHEDULED:
					return newState == INITIALIZING || newState == NEW;
				default:
					return false;
				}
			}
		}

		@Override
		public String getName() {
			return toString();
		}
	}

	/**
	 * The actual state of the job. This variable is volatile, because it will
	 * be accessed from the JobThread and has do be kept synchronous.
	 */
	protected volatile JobState state;

	/**
	 * Initialize the state of the job with NEW.
	 */
	protected Job() {
		state = State.NEW;
	}

	/**
	 * Converts a string into the appropriate {@link JobState}.
	 *
	 * @param str
	 *            the job state as a string
	 * @return the JobState
	 */
	public static JobState convertStateFromString(String str) {
		try {
			return State.valueOf(str.toUpperCase());
		} catch (IllegalArgumentException ex) {
			return new CustomState(str);
		}
	}

	/**
	 * This method sets the state. If the state changes, the observers will be
	 * notified.
	 *
	 * Only some transitions are allowed, see {@link #isNewStateAllowed(String)}
	 * . For illegal transitions a {@link IllegalStateTransition} is thrown.
	 *
	 * @param newState
	 *            the new state of the job
	 * @throws IllegalStateTransition
	 *             for illegal transitions
	 */
	public synchronized void setState(JobState newState) {
		if (!state.equals(newState)) {
			if (!state.isStateTransitionAllowed(newState)) {
				throw new IllegalStateTransition(this.state, newState);
			}
			this.state = newState;
			this.setChanged();
			getLogger().debug("changed to state %s", this.state.toString());
			this.notifyObservers(newState);
			if (state.isFinal()) {
				clean();
			}
		}
	}

	/**
	 * Set the state of the current job to the new state.
	 * <p>
	 * Note, this overloaded function will only work on a running job.
	 *
	 * @param newState
	 *            the custom state
	 * @see Job#setState(JobState)
	 */
	public synchronized void setState(String newState) {
		setState(new CustomState(newState));
	}

	/**
	 * Clean up temporary files and alike.
	 */
	protected abstract void clean();

	/**
	 * Returns the UUID identifying the job (and later the measured data).
	 *
	 * @return the UUID as a string
	 */
	public abstract String getID();

	/**
	 * Returns a ready to use logger for the job. This is intended to be used by
	 * algorithms and parsers that want to log. Uses log4j.
	 *
	 * @return a log4j logger
	 * @see {@linkplain http://logging.apache.org/log4j/2.x/manual/index.html}
	 */
	public abstract Logger getLogger();

	/**
	 * This method returns the descriptor string of the parser.
	 *
	 * @return descriptor string of the parser
	 */
	public abstract String getParserDescriptor();

	/**
	 * @return the parameters of the algorithm(s)
	 */
	public abstract Map<String, Object> getParameters();

	/**
	 * Provides the parsed graph.
	 *
	 * @return a graph parsed from the given input graph path.
	 */
	public abstract Object getInputGraph(); // TODO Graph instead of object?

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Job)) {
			return false;
		}
		Job job = (Job) obj;
		return job.getID().equalsIgnoreCase(getID());
	}

	@Override
	public String toString() {
		return "Job-" + getID() + "(" + getState().getName() + ")";
	}

	/**
	 * @return the parser parameters
	 */
	public abstract Map<String, Object> getParserParameters();

	/**
	 * @return the state of the job
	 */
	protected JobState getState() {
		return state;
	}

}
