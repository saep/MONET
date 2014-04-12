package com.github.monet.common.logging;

import org.apache.logging.log4j.Level;

/**
 * A filter for LogEvents. Use this with a {@link LoggingPublisher} to receive
 * only certain logging events.
 *
 * @author Max Günther
 *
 */
public class LoggingFilter implements LoggingListener {
	/**
	 *
	 */
	private static final long serialVersionUID = -7541721331156119372L;
	private LoggingListener listener;
	private Level minLevel;

	public LoggingFilter(Level minLevel) {
		super();
		this.minLevel = minLevel;
	}

	/**
	 * Returns true, if the LoggingEvent passes through the filter.
	 *
	 * @param event
	 *            the event to filter
	 * @return true if the event passes the filter
	 */
	public boolean filter(LogEvent event) {
		return this.minLevel.isAtLeastAsSpecificAs(event.getLevel());
	}

	public Level getMinLevel() {
		return this.minLevel;
	}

	public void setMinLevel(Level minLevel) {
		this.minLevel = minLevel;
	}

	public LoggingListener getListener() {
		return this.listener;
	}

	public void setListener(LoggingListener listener) {
		this.listener = listener;
	}

	@Override
	public void logEvent(LogEvent event) {
		if ((this.listener != null) && this.filter(event)) {
			this.listener.logEvent(event);
		}
	}

}
