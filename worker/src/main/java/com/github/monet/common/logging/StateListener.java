package com.github.monet.common.logging;

/**
 * Interface that allows to listen to StateEvents.
 *
 * @author Marco Kuhnke
 *
 */
public interface StateListener {

	/**
	 * Called when a StateEvent was received.
	 *
	 * @param event
	 *            the event that occurred
	 */
	public void logStateEvent(StateEvent event);
}
