/**
 *
 */
package com.github.monet.worker;

import java.io.Serializable;

import com.github.monet.worker.Job.State;

/**
 * The state of the job.
 * <p>
 * This interface has a public subclass that defines a custom state. If you need
 * a predefined state, it can be found in the state enumeration in Job.
 *
 * @see CustomState
 * @see State
 */
public interface JobState extends Serializable {
	/**
	 * Returns true if the state transition is allowed, false otherwise.
	 *
	 * Only these transitions are allowed:
	 * <ul>
	 * <li>{@link Job#STATE_NEW} to {@link Job#STATE_SCHEDULED}</li>
	 * <li>{@link Job#STATE_SCHEDULED} to {@link Job#STATE_INITIALISING}</li>
	 * <li>{@link Job#STATE_INITIALISING} to {@link Job#STATE_PARSING}</li>
	 * <li>{@link Job#STATE_PARSING} to {@link Job#STATE_RUNNING}/li>
	 * <li>{@link Job#STATE_RUNNING} to any custom state</li>
	 * <li>any custom state to any custom state</li>
	 * <li>any custom state to any state in {@link Job#FINISH_STATES}</li>
	 * <li>{@link Job#STATE_RUNNING} to any state in {@link Job#FINISH_STATES}</li>
	 * <li>from any state not in {@link Job#FINISH_STATES} to
	 * {@link Job#STATE_FAILED}</li>
	 * <li>from any state not in {@link Job#FINISH_STATES} to
	 * {@link Job#STATE_ABORTED}</li>
	 * </ul>
	 *
	 * @param newState
	 *            the state to transition into
	 * @return true if the transition is allowed, false otherwise
	 */
	boolean isStateTransitionAllowed(JobState newState);

	/**
	 * @return true if the state is a final state
	 */
	boolean isFinal();

	/**
	 * @return the name of the state
	 */
	String getName();

	/**
	 * Simple class object that stores a custom state string and is compatible
	 * with the internal state objects.
	 */
	public static class CustomState implements JobState {
		/**
		 *
		 */
		private static final long serialVersionUID = -818027424363161405L;
		private String name;

		/**
		 * Create a custom state object with the given name.
		 *
		 * @param name
		 *            the name of the state
		 *
		 * @see JobState
		 */
		public CustomState(String name) {
			this.name = name;
		}

		@Override
		public boolean isStateTransitionAllowed(JobState newState) {
			return Job.State.RUNNING.isStateTransitionAllowed(newState);
		}

		@Override
		public boolean isFinal() {
			return false;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
