package com.github.monet.common.logging;

import java.io.Serializable;

/**
 * Interface that allows to listen to any kind of LogEvents.
 *
 * @author Max Günther
 *
 */
public interface LoggingListener extends Serializable {

	/**
	 * Called when a LogEvent was received.
	 *
	 * @param event
	 *            the event that occured
	 */
	public void logEvent(LogEvent event);
}
