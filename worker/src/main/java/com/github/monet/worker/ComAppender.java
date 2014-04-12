package com.github.monet.worker;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.DefaultErrorHandler;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.github.monet.common.logging.LoggingPublisher;

/**
 * This class is an log4j 2 appender. It receives logging messages from log4j 2
 * and dispatches them to the appropriate channels.
 *
 * This class requires the "Log4j2Plugins.dat" so log4j2 can find and install
 * the plugin defined in this class. This requires the worker project to be
 * built at least once - otherwise this file won't exist.
 *
 * @author Max Günther
 *
 */
@Plugin(name = "ComAppender", category = "Core", elementType = "appender", printObject = true)
public final class ComAppender implements Appender {
	private String name;
	/**
	 * Layout used for sending plain message via the communicator.
	 */
	private Layout<String> bareLayout;
	/**
	 * Layout used for all messages not send via communicator.
	 */
	private Layout<String> defaultLayout;
	private boolean started;
	private ErrorHandler errorHandler;
	/**
	 * Should be set during start up of the worker.
	 */
	public static Communicator centralCom;
	/**
	 * Should be set during start up of the controlserver (if one is started).
	 */
	public static LoggingPublisher controlPublisher;

	private ComAppender(String name, Layout<String> defaultLayout,
			boolean handleExceptions) {
		this.name = name;
		this.bareLayout = PatternLayout.createLayout("%msg", null, null,
				null,
				null);
		this.defaultLayout = defaultLayout;
		this.started = false;
		this.errorHandler = new DefaultErrorHandler(this);
	}

	@PluginFactory
	public static ComAppender createAppender(
			@PluginAttribute("name") String name,
			@PluginElement("layout") Layout<String> defaultLayout,
			@PluginAttribute("suppressExceptions") String suppress) {
		boolean handleExceptions = suppress == null ? true : Boolean
				.valueOf(suppress);

		assert name != null : "no name provided for ComAppender";
		if (defaultLayout == null) {
			defaultLayout = PatternLayout.createLayout(
					"%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n",
					null, null, null, null);
		}
		return new ComAppender(name, defaultLayout, handleExceptions);
	}

	@Override
	public boolean isStarted() {
		return this.started;
	}

	@Override
	public void start() {
		this.started = true;
	}

	@Override
	public void stop() {
		// Do nothing
	}

	@Override
	public void append(LogEvent event) {
		if (centralCom == null) {
			String message = this.defaultLayout.toSerializable(event);
			System.out.print(message);
			return;
		}
		String threadName = event.getThreadName();
		String channel = null;
		if (event.getLoggerName().startsWith("job")
				&& !threadName.startsWith("server")) {
			String message = this.bareLayout.toSerializable(event);
			channel = event.getLoggerName();
			centralCom.sendLoggingMessage(channel, event.getMillis(),
					threadName, event.getLevel(),
					event.getLoggerName(), message, event.getThrown());
		} else if (threadName.startsWith("JobThread")
				|| threadName.equals("worker")
				|| threadName.equals("communicator")) {
			String message = this.bareLayout.toSerializable(event);
			channel = "worker";
			centralCom.sendLoggingMessage(channel, event.getMillis(),
					threadName, event.getLevel(),
					event.getLoggerName(), message, event.getThrown());
		} else if (controlPublisher != null) {
			String message = this.bareLayout.toSerializable(event);
			com.github.monet.common.logging.LogEvent logEvent = new com.github.monet.common.logging.LogEvent(
					"control", event.getMillis(), threadName, event.getLevel()
							.toString(), event.getLoggerName(), message,
					event.getThrown());
			controlPublisher.publishLoggingEvent(logEvent);
		} else {
			// some other message; should never occur
			String message = this.defaultLayout.toSerializable(event);
			System.out.print("control: " + message);
		}

	}

	@Override
	public ErrorHandler getHandler() {
		return this.errorHandler;
	}

	@Override
	public Layout<String> getLayout() {
		return this.bareLayout;
	}

	@Override
	public String getName() {
		return this.name;
	}


	@Override
	public void setHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public boolean ignoreExceptions() {
		return true;
	}
}
