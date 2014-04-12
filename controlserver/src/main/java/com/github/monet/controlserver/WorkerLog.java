package com.github.monet.controlserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
 * @author Johannes Kowald
 */
public class WorkerLog implements LoggingListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 8838997226460983502L;

	public final static int BUFFER_SIZE = 100;

	private List<LogEvent> buffer;

	private String workerName;

	public WorkerLog(String name) {
		this.workerName = name;
		this.buffer = Collections.synchronizedList(new ArrayList<LogEvent>(
				BUFFER_SIZE));
	}

	@Override
	public synchronized void logEvent(LogEvent event) {
		this.buffer.add(event);
		if (this.buffer.size() >= BUFFER_SIZE) {
			flush();
		}
	}

	/**
	 * Returns an iterator over all {@link LogEvent}s of this worker log.
	 *
	 * @return the iterator over all LogEvents
	 */
	public Iterator<LogEvent> iterator() {
		return new ConjoinedIterator<>(getSerializedMessages(),
				getBufferedMessages());
	}

	/**
	 * Returns a list of all buffered {@link LogEvent}s of this worker log.
	 *
	 * @return the list of all LogEvents
	 */
	public List<LogEvent> getBufferedMessages() {
		return this.buffer;
	}

	/**
	 * Returns a list with all {@link LogEvent}s on MongoDB of this worker log.
	 *
	 * @return the list of all serialized LogEvents
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<LogEvent> getSerializedMessages() {
		DB db = ControlServer.getInstance().db;
		DBCollection workers = db.getCollection(DBCollections.WORKERS);
		DBObject worker = workers.findOne(new BasicDBObject("_id", workerName),
				new BasicDBObject("log", true));
		List<LogEvent> events = new ArrayList<>();
		if (worker != null) {
			for (DBObject dbLogEvent : (List<DBObject>) worker.get("log")) {
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
	 * Flushes the buffer to MongoDB.
	 */
	public synchronized void flush() {
		DB db = ControlServer.getInstance().db;
		DBCollection workers = db.getCollection(DBCollections.WORKERS);
		DBObject worker = new BasicDBObject("_id", workerName);
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
		workers.update(worker, updateObj);
		buffer.clear();
	}
}
