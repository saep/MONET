package com.github.monet.common.logging;

import java.io.Serializable;

import org.apache.logging.log4j.Level;

import com.github.monet.common.ExceptionUtil;
import com.mongodb.BasicDBObject;

public class LogEvent implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 3630592333655683001L;
	private String channel;
	private long millis;
	private String threadName;
	private Level level;
	private String logger;
	private String message;
	private String errorName;
	private String errorMessage;
	private String errorStacktrace;

	public LogEvent(String channel, long millis, String threadName,
			String level, String logger, String message) {
		super();
		this.channel = channel;
		this.millis = millis;
		this.threadName = threadName;
		this.level = Level.toLevel(level);
		this.logger = logger;
		this.message = message;
	}

	public LogEvent(String channel, long millis, String threadName,
			String level,
			String logger, String message, Throwable error) {
		this(channel, millis, threadName, level, logger, message);
		if (error != null) {
			this.errorName = error.getClass().getName();
			this.errorMessage = error.getMessage();
			this.errorStacktrace = ExceptionUtil.stacktraceToString(error);
		}
	}

	public LogEvent(String channel, long millis, String threadName,
			String level,
			String logger, String message, String errorName,
			String errorMessage, String errorStacktrace) {
		this(channel, millis, threadName, level, logger, message);
		if ((errorName != null) && errorName.equals("")) {
			this.errorName = null;
			this.errorMessage = null;
			this.errorStacktrace = null;
		}
		this.errorName = errorName;
		this.errorMessage = errorMessage;
		this.errorStacktrace = errorStacktrace;
	}

	public String getChannel() {
		return this.channel;
	}

	public long getMillis() {
		return this.millis;
	}

	public String getThreadName() {
		return this.threadName;
	}

	public Level getLevel() {
		return this.level;
	}

	public String getLogger() {
		return this.logger;
	}

	public String getMessage() {
		return this.message;
	}

	public String getErrorName() {
		return this.errorName;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public String getErrorStacktrace() {
		return this.errorStacktrace;
	}

	@Override
	public String toString() {
		if (this.errorName != null) {
			return String.format("%s: [%s] %s %s - %s\nStacktrace:\n%s",
					this.channel, this.threadName, this.level, this.logger,
					this.message, this.errorStacktrace);
		} else {
			return String.format("%s: [%s] %s %s - %s", this.channel,
					this.threadName, this.level, this.logger, this.message);
		}
	}

	public BasicDBObject toDBObject() {
		BasicDBObject eventObject = new BasicDBObject();
		eventObject.put("channel", channel);
		eventObject.put("millis", millis);
		eventObject.put("threadname", threadName);
		eventObject.put("level", level.toString());
		eventObject.put("logger", logger);
		eventObject.put("message", message);
		eventObject.put("errorname", errorName);
		eventObject.put("errormessage", errorMessage);
		eventObject.put("errorstacktrace", errorStacktrace);
		return eventObject;
	}

}
