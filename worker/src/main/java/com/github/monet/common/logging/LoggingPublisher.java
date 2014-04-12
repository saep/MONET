package com.github.monet.common.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Allows publishing and subscribing to {@link LoggingEvents}.
 *
 * @author Max Günther
 *
 */
public class LoggingPublisher implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -491757214585669124L;
	private ArrayList<LoggingListener> loggingListeners;

	public LoggingPublisher() {
		this.loggingListeners = new ArrayList<LoggingListener>();
	}

	/**
	 * Add a LoggingListener to listen to all events published using this
	 * LoggingPublisher.
	 *
	 * @param listener
	 *            the listener to receive the events
	 */
	public void addLoggingListener(LoggingListener listener) {
		this.loggingListeners.add(listener);
	}

	/**
	 * Add a LoggingListener to listen to events published using this
	 * LoggingPublisher with a filter used to let only specific messages pass.
	 *
	 * @param listener
	 *            the listener to receive the events
	 * @param filter
	 *            a filter that lets only specific logging events pass
	 */
	public void addLoggingListener(LoggingListener listener,
			LoggingFilter filter) {
		filter.setListener(listener);
		this.loggingListeners.add(filter);
	}

	/**
	 * Remove a LoggingListener.
	 *
	 * @param listener
	 *            the listener to no longer receive events
	 */
	public void removeLoggingListener(LoggingListener listener) {
		Iterator<LoggingListener> it = this.loggingListeners.iterator();
		while (it.hasNext()) {
			LoggingListener l = it.next();
			if (l instanceof LoggingFilter) {
				LoggingFilter filter = (LoggingFilter) l;
				if (filter == listener) {
					it.remove();
				}
			} else if (l == listener) {
				it.remove();
			}
		}
	}

	/**
	 * Publish a LogEvent to all listeners.
	 *
	 * @param event
	 *            the logging event to publish
	 */
	public void publishLoggingEvent(LogEvent event) {
		for (LoggingListener l : this.loggingListeners) {
			l.logEvent(event);
		}
	}

}
