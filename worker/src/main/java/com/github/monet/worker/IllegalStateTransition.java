package com.github.monet.worker;


/**
 * Runtime exception thrown whenever {@link Job} makes an illegal state
 * transition.
 *
 * @author Max Günther
 *
 */
public class IllegalStateTransition extends RuntimeException {
	private static final long serialVersionUID = 4453118778157298361L;

	public IllegalStateTransition(JobState oldState, JobState newState) {
		super(String.format("illegal transition from state %s to %s",
				oldState.getName(), newState.getName()));
	}

}
