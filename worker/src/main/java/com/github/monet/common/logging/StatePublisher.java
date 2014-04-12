package com.github.monet.common.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Allows publishing and subscribing to {@link StateEvent}s.
 *
 * @author Marco Kuhnke
 */
public class StatePublisher implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 8805526338972273809L;
	private ArrayList<StateListener> stateListeners;

	public StatePublisher() {
		this.stateListeners = new ArrayList<StateListener>();
	}

	/**
	 * Adds a {@link StateListener} to listen to all events published
	 * using this {@code StatePublisher}.
	 *
	 * @param listener
	 *            the listener to receive the {@link StateEvent}s
	 */
	public void addLoggingListener(StateListener listener) {
		this.stateListeners.add(listener);
	}

	/**
	 * Removes a {@link StateListener}.
	 *
	 * @param listener
	 *            the listener to no longer receive [@link StateEvent}s
	 */
	public void removeLoggingListener(StateListener listener) {
		Iterator<StateListener> it = this.stateListeners.iterator();
		while (it.hasNext()) {
			if (it.next() == listener) {
				it.remove();
			}
		}
	}

	/**
	 * Publish a {@link StateEvent} to all listeners.
	 *
	 * @param event
	 *            the {@code StateEvent} to publish
	 */
	public void publishLoggingEvent(StateEvent event) {
		for (StateListener l : this.stateListeners) {
			l.logStateEvent(event);
		}
	}

}
