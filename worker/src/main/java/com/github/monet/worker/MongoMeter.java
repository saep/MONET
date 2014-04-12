package com.github.monet.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.aggregators.Aggregator;
import com.github.monet.common.DBCollections;
import com.github.monet.common.MongoBuilder;
import com.github.monet.common.MongoBuilderException;
import com.github.monet.interfaces.MeasurementStream;
import com.github.monet.interfaces.Meter;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * This class is used to measure and record values during the execution of an
 * algorithm using <a href="http://www.mongodb.com">MongoDB</a>.
 *
 * <p>
 * There are two methods of measuring: using the <code>measure*(..)</code>
 * -method and using a {@link MeasurementStream}. While the latter is obtained
 * through the {@link #getMeasurementStream()}-method in this class its
 * documentation is in its own class. This document only explains the
 * <code>measure*(..)</code>-methods.
 * </p>
 * <p>
 * The advantage of this method is that all data can be easily queried. However
 * only up to 16MB can be recorded per experiment (one execution of one
 * algorithm).
 * </p>
 * <p>
 * All these methods (like {@link #measureInt(String, int)}) use a String
 * parameter that specifies a path where the second parameter (the value) is to
 * be saved. These path expressions are build like regular file path
 * expressions, but don't actually correspond to files on a filesystem, but to
 * entries in a JSON document. Since all the expressions are absolute, the "/"
 * at the first position can be omitted. For lists or arrays there is a special
 * path element: "<code>#n</code>", where <code>n</code> is the index of value
 * in the list. <code>n</code> can be omitted in order to append a value to the
 * list. Indices start with 0.
 * </p>
 * Example:
 *
 * <pre>
 * <code>
 * mesaureInt("someint", 4711);
 * measureInt("mykey/foo/bar", 1);
 * measureInt("mykey/foo/bar2", 2);
 * measureDouble("bla/#", 0.1);
 * measureDouble("bla/#", 0.2);
 * measureDouble("bla/#1", 0.3);
 * measureString("bla/#/dumm", "dumm");
 * measureString("bla/#/dumm2", "dumm2");
 * measureString("bla/#3/dumm2", "dumm2 overwritten");
 * measureString("bla/#3/muh", "second item");
 * </code>
 * </pre>
 *
 * Result:
 *
 * <pre>
 * <code>
 * {"someint": 4711,
 *  "mykey"  : {"foo": {"bar": 1,
 *                      "bar2": 2}},
 *  "bla"    : [0.1,
 *              0.3,
 *              {"dumm": "dumm"},
 *              {"dumm2": "dumm2 overwritten",
 *               "muh" : "second item"}],
 * }
 * </code>
 * </pre>
 *
 * <h2>Conventions and reserved paths</h2>
 * <p>
 * For complete explanation see {@link Meter}.
 * </p>
 *
 * @author Max Günther
 * @see TestMeter
 */
public class MongoMeter implements Meter {
	private final static Logger log = LogManager
			.getFormatterLogger(MongoMeter.class);
	private DB db;
	private DBCollection jobs;
	private MeasurementStream measurementStream = null;
	private MongoBuilder builder;
	private Map<String, Long> timers;
	private int paretoIndex = 0;
	private String jobID;
	private Map<String, Aggregator<Object>> aggregators;
	private boolean finished = false;

	/**
	 * Creates a new meter using a mongo database and the ID of the job.
	 *
	 * @param db
	 *            the mongo db object
	 * @param jobID
	 *            the id of the job
	 */
	MongoMeter(DB db, String jobID) {
		this.db = db;
		this.jobs = db.getCollection(DBCollections.JOBS);
		this.jobID = jobID;
		// create the measurement stream
		try {
			this.measurementStream = new MongoMeasurementStream(this.db,
					this.jobID.replaceAll("/", "-"));
		} catch (IOException e) {
			log.error(e);
		}
	}

	@Override
	public void startExperiment() {
		this.builder = new MongoBuilder();
		this.timers = new HashMap<String, Long>();
		this.aggregators = new HashMap<String, Aggregator<Object>>();
		this.startTimer("RUNTIME");
	}

	@Override
	public void endExperiment() {
		if (!finished) {
			this.stopTimer("RUNTIME");
			finished = true;
		}
	}

	@Override
	public void send() {
		for (Entry<String, Aggregator<Object>> item : this.aggregators
				.entrySet()) {
			this.builder.insert(item.getKey(), item.getValue().getValue());
		}
		BasicDBObject updateObj = new BasicDBObject("$set", new BasicDBObject(
				"measuredData", this.builder));
		this.jobs.update(new BasicDBObject("_id", jobID), updateObj);
		this.measurementStream.saveFile();
		this.timers.clear();
		this.builder = null;
	}

	@Override
	public void measureInt(String path, int value) {
		this.measure(path, value);
	}

	@Override
	public void measureLong(String path, long value) {
		this.measure(path, value);
	}

	@Override
	public void measureDouble(String path, double value) {
		this.measure(path, value);
	}

	@Override
	public void measureString(String path, String value) {
		this.measure(path, value);
	}


	@Override
	public void startTimer(String path) {
		this.timers.put(path, System.nanoTime());
	}

	@Override
	public void stopTimer(String path) {
		long endTime = System.nanoTime();
		long startTime = this.timers.remove(path);
		long elapsedTime = endTime - startTime;
		this.measureLong(path, elapsedTime);
	}

	@Override
	public MeasurementStream getMeasurementStream() {
		return this.measurementStream;
	}

	private String appendListSymbol(String path) {
		if (path.endsWith("/")) {
			return path + "#";
		} else {
			return path + "/#";
		}
	}

	@Override
	public void measureDouble(String path, Iterable<Double> values) {
		path = appendListSymbol(path);
		for (double v : values) {
			this.measure(path, v);
		}
	}

	@Override
	public void measureInt(String path, Iterable<Integer> values) {
		path = appendListSymbol(path);
		for (int v : values) {
			this.measure(path, v);
		}
	}

	@Override
	public void measureInt(String path, int[] arr) {
		path = appendListSymbol(path);
		for (int v : arr) {
			this.measure(path, v);
		}
	}

	@Override
	public void measureLong(String path, Iterable<Long> values) {
		path = appendListSymbol(path);
		for (long v : values) {
			this.measure(path, v);
		}
	}

	@Override
	public void measureLong(String path, long[] values) {
		path = appendListSymbol(path);
		for (long v : values) {
			this.measure(path, v);
		}
	}

	@Override
	public void measureDouble(String path, double[] arr) {
		path = appendListSymbol(path);
		for (double v : arr) {
			this.measure(path, v);
		}
	}

	public void measure(String path, Object value) {
		Aggregator<Object> agg = this.aggregators.get(path);
		if (agg == null) {
			this.builder.insert(path, value);
		} else {
			agg.aggregate(value);
		}
	}

	@Override
	public void addAggregator(String path, Aggregator<Object> agg) {
		this.aggregators.put(path, agg);
	}

	@Override
	public void measurePareto(double[] point, Collection<String> edges) {
		this.measure("paretoFront/points/#/", point);
		if (edges != null) {
			this.measure("paretoFront/edges/#/", edges);
		}
		paretoIndex++;
	}

	@Override
	public List<double[]> getParetoFront() {
		List<double[]> ret = new ArrayList<>();
		try {
			DBObject front = (DBObject) builder.get("paretoFront");
			@SuppressWarnings("unchecked")
			List<double[]> points = (List<double[]>) front
					.get("points");
			for (double[] p : points) {
				double point[] = new double[p.length];
				int i = 0;
				for (double d : p) {
					point[i] = d;
					i++;
				}
				ret.add(point);
			}
			return ret;
		} catch (MongoBuilderException ex) {
			return null;
		}
	}
}
