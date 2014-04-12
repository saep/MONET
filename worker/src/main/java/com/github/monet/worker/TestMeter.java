package com.github.monet.worker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.monet.aggregators.Aggregator;
import com.github.monet.interfaces.MeasurementStream;
import com.github.monet.interfaces.Meter;

/**
 * This Meter simply writes the measured data to the console.
 *
 * @author Max Günther
 * @see Meter, MongoMeter
 *
 */
public class TestMeter implements Meter {
	private MeasurementStream measurementStream = null;
	private Map<String, Long> timers;
	private Map<String, Aggregator<Object>> aggregators;
	private Collection<ParetoPoint> paretoFront;

	public TestMeter() {
		this.measurementStream = new TestMeasurementStream();
		this.paretoFront = new ArrayList<>();
	}

	@Override
	public void startExperiment() {
		this.aggregators = new HashMap<String, Aggregator<Object>>();
		this.timers = new HashMap<String, Long>();
		System.out.println("started experiment");
	}

	@Override
	public void endExperiment() {
		System.out.println("Pareto Front:");
		for (ParetoPoint point : this.paretoFront) {
			String str = String.format("%f: %s", Arrays.toString(point.point),
					point.edges);
			System.out.println(str);
		}
		System.out.println("finished experiment");
	}

	@Override
	public void measureInt(String path, int value) {
		this.measure(path, String.valueOf(value));
	}

	@Override
	public void measureLong(String path, long value) {
		this.measure(path, String.valueOf(value));
	}

	@Override
	public void measureDouble(String path, double value) {
		this.measure(path, String.valueOf(value));
	}

	@Override
	public void measureString(String path, String value) {
		this.measure(path, String.valueOf(value));
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

	@Override
	public void measureInt(String path, Iterable<Integer> val) {
		for (Integer i : val) {
			this.measure(path, String.valueOf(i));
		}
	}

	@Override
	public void measureDouble(String path, Iterable<Double> val) {
		for (Double d : val) {
			this.measure(path, String.valueOf(d));
		}
	}

	@Override
	public void measureInt(String path, int[] arr) {
		for (int i : arr) {
			this.measure(path, String.valueOf(i));
		}
	}

	@Override
	public void measureLong(String path, Iterable<Long> val) {
		for (Long l : val) {
			this.measure(path, String.valueOf(l));
		}
	}

	@Override
	public void measureLong(String path, long[] values) {
		for (long l : values) {
			this.measure(path, String.valueOf(l));
		}
	}

	@Override
	public void measureDouble(String path, double[] arr) {
		for (double d : arr) {
			this.measure(path, String.valueOf(d));
		}
	}

	public void measure(String path, Object value) {
		Aggregator<Object> agg = this.aggregators.get(path);
		if (agg != null) {
			agg.aggregate(value);
			value = agg.getValue();
		}
		System.out.format("%s: %s\n", path, value.toString());
	}

	@Override
	public void addAggregator(String path, Aggregator<Object> agg) {
		this.aggregators.put(path, agg);
	}

	@Override
	public void measurePareto(double[] point, Collection<String> edges) {
		ParetoPoint p = new ParetoPoint();
		p.point = point;
		p.edges = edges;
		this.paretoFront.add(p);
	}

	private class ParetoPoint {
		double[] point;
		Collection<String> edges;
	}

	@Override
	public List<double[]> getParetoFront() {
		ArrayList<double[]> front = new ArrayList<>();
		for (ParetoPoint p : paretoFront) {
			front.add(p.point);
		}
		return front;
	}

	@Override
	public void send() {
		// don't do anything
	}
}
