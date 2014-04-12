package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.github.monet.common.DBCollections;
import com.github.monet.common.logging.LogEvent;
import com.github.monet.common.logging.LoggingListener;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * Access the log of a job.
 *
 * @author Max Günther
 *
 */
public class CSLog implements LoggingListener, Observer, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 2144147821008559448L;
	private String jobID;
	private List<LogEvent> buffer;
	public final static int BUFFER_SIZE = 100;

	/**
	 * Create a CSLog for a job.
	 *
	 * @param jobID
	 *            the ID of the job for the log
	 */
	public CSLog(String jobID) {
		this.jobID = jobID;
		this.buffer = Collections.synchronizedList(new ArrayList<LogEvent>(
				BUFFER_SIZE));
	}

	/**
	 * Returns an iterator over all {@link LogEvent}s for this logs job.
	 *
	 * @return the iterator over all LogEvents
	 */
	public synchronized Iterator<LogEvent> iterator() {
		return new ConjoinedIterator<>(getSerializedMessages(),
				getBufferedMessages());
	}

	/**
	 * Returns a list of all buffered {@link LogEvent}s of this logs job.
	 *
	 * @return the list of all LogEvents
	 */
	public synchronized List<LogEvent> getBufferedMessages() {
		return this.buffer;
	}

	/**
	 * Returns a list with all {@link LogEvent}s on MongoDB for this logs job.
	 *
	 * @return the list of all serialized LogEvents
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<LogEvent> getSerializedMessages() {
		DB db = ControlServer.getInstance().db;
		DBCollection jobs = db.getCollection(DBCollections.JOBS);
		DBObject job = jobs.findOne(new BasicDBObject("_id", jobID),
				new BasicDBObject("log", true));
		List<LogEvent> events = new ArrayList<>();
		if (job != null) {
			for (DBObject dbLogEvent : (List<DBObject>) job.get("log")) {
				String channel = (String) dbLogEvent.get("channel");
				long millis = (long) dbLogEvent.get("millis");
				String threadName = (String) dbLogEvent.get("thread");
				String level = (String) dbLogEvent.get("level");
				String logger = (String) dbLogEvent.get("logger");
				String message = (String) dbLogEvent.get("message");
				String errorName = (String) dbLogEvent.get("errorName");
				String errorMessage = (String) dbLogEvent.get("errorMessage");
				String errorStacktrace = (String) dbLogEvent
						.get("errorStacktrace");
				LogEvent event = new LogEvent(channel, millis, threadName,
						level, logger, message, errorName, errorMessage,
						errorStacktrace);
				events.add(event);
			}
		}
		return events;
	}

	/**
	 * Places an event in the buffer and flushes it if necessary.
	 */
	@Override
	public synchronized void logEvent(LogEvent event) {
		this.buffer.add(event);
		if (this.buffer.size() >= BUFFER_SIZE) {
			flush();
		}
	}

	/**
	 * Flushes the buffer to MongoDB.
	 */
	public synchronized void flush() {
		DB db = ControlServer.getInstance().db;
		DBCollection jobs = db.getCollection(DBCollections.JOBS);
		DBObject job = new BasicDBObject("_id", jobID);
		BasicDBList dbLogEvents = new BasicDBList();
		for (LogEvent e : this.buffer) {
			BasicDBObject dbLogEvent = new BasicDBObject();
			dbLogEvent.put("logger", e.getLogger());
			dbLogEvent.put("thread", e.getThreadName());
			dbLogEvent.put("channel", e.getChannel());
			dbLogEvent.put("message", e.getMessage());
			dbLogEvent.put("errorMessage", e.getErrorMessage());
			dbLogEvent.put("errorName", e.getErrorName());
			dbLogEvent.put("errorStacktrace", e.getErrorStacktrace());
			dbLogEvent.put("level", e.getLevel().toString());
			dbLogEvent.put("millis", e.getMillis());
			dbLogEvents.add(dbLogEvent);
		}
		BasicDBObject updateObj = new BasicDBObject("$push", new BasicDBObject(
				"log", new BasicDBObject("$each", dbLogEvents)));
		jobs.update(job, updateObj);
		buffer.clear();
	}

	/**
	 * Called whenever the logs job changes its state and flushes the buffer.
	 */
	@Override
	public void update(Observable obs, Object state) {
		flush();
	}
}
