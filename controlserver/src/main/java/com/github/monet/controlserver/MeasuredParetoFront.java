package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.DBObject;

/**
 * Retrieve pareto front data that has been measured for a job or experiment.
 *
 * @author Johannes Kowald
 */
public class MeasuredParetoFront implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -3275764777163285644L;

	private DBObject paretoFrontObject;

	List<List<Double>> pointList;

	List<List<String>> edgeList;

	public MeasuredParetoFront(DBObject paretoFrontObject) {
		this.paretoFrontObject = paretoFrontObject;
	}

	@SuppressWarnings("unchecked")
	public List<List<Double>> getParetoFrontPoints() {
		if (pointList == null) {
			pointList = new ArrayList<List<Double>>();
			List<DBObject> pointObjectList = (List<DBObject>) paretoFrontObject.get("points");
			for (DBObject singlePointAsObject : pointObjectList) {
				pointList.add((List<Double>) singlePointAsObject);
			}
		}
		return this.pointList;
	}

	@SuppressWarnings("unchecked")
	public List<List<String>> getParetoFrontEdges() {
		if (edgeList == null) {
			edgeList = new ArrayList<List<String>>();
			List<DBObject> edgeObjectList = (List<DBObject>) paretoFrontObject.get("edges");
			for (DBObject singleEdgeAsObject : edgeObjectList) {
				edgeList.add((List<String>) singleEdgeAsObject);
			}
		}
		return this.edgeList;
	}

	public Double getValue(int x, int y) throws IndexOutOfBoundsException {
		List<Double> sndDim = getParetoFrontPoints().get(x);
		return sndDim.get(y);
	}

	public Integer getDimension() {
		return this.getParetoFrontPoints().get(0).size();
	}

	public Boolean is2Dimensional() {
		return (this.getDimension() == 2);
	}

}
