package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.monet.common.DBCollections;
import com.github.monet.common.MongoBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Retrieve data that has been measured for a job or experiment.
 *
 * @author Max Günther
 * @author Johannes Kowald
 *
 */
public class MeasuredData implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -8443116127047618900L;
	private List<DBObject> dataList;

	/**
	 * Create the MeasuredData object for a particular request. Note that you
	 * should not keep a reference to this object to long, but simply make a new
	 * one whenever needed - otherwise the measured data will become stale.
	 *
	 * @param query
	 *            A {@link BasicDBObject} which specifies the query parameters
	 */
	public MeasuredData(BasicDBObject query) {
		this.dataList = new ArrayList<DBObject>();
		DB db = ControlServer.getInstance().db;
		DBCollection jobCollection = db.getCollection(DBCollections.JOBS);
		DBCursor jobCursor = jobCollection.find(query, new BasicDBObject(
				"measuredData", true));
		while (jobCursor.hasNext()) {
			DBObject job = jobCursor.next();
			dataList.add((DBObject) job.get("measuredData"));
		}
	}

	/**
	 * Return the runtime of the job in nanoseconds.
	 *
	 * @return the runtime in nanoseconds
	 */
	public List<Long> getRuntimes() {
		List<Long> runtimeList = new ArrayList<Long>();
		for (DBObject data : dataList) {
			runtimeList.add((Long) data.get("RUNTIME"));
		}
		return runtimeList;
	}

	/**
	 * Returns the a list of pareto front object which provide access to all
	 * information of the pareto front.
	 *
	 * @return A list of pareto front objects
	 */
	public List<MeasuredParetoFront> getParetoFronts() {
		List<MeasuredParetoFront> paretoFrontList = new ArrayList<MeasuredParetoFront>();
		for (DBObject data : dataList) {
			MeasuredParetoFront measuredPF = new MeasuredParetoFront(
					(DBObject) data.get("paretoFront"));
			paretoFrontList.add(measuredPF);
		}
		return paretoFrontList;
	}

	/**
	 * Returns the list if S-Metric values for each compared job. Note that
	 * individual values within the list might be <code>null</code> because no
	 * pareto front was measured for that job.
	 *
	 * @return the S-Metric values
	 */
	public List<Double> getSMetrics() {
		List<Double> metrics = new ArrayList<>();
		for (DBObject data : dataList) {
			Double metric = (Double) data.get("SMetric");
			metrics.add(metric);
		}
		return metrics;
	}

	/**
	 * Find any measured data given a path expression.
	 *
	 * @param path
	 *            the path expression
	 * @return the measured data
	 */
	public List<Object> find(String path) {
		List<Object> result = new ArrayList<Object>();
		for (DBObject data : dataList) {
			result.add(MongoBuilder.find(data, path));
		}
		return result;
	}
}
