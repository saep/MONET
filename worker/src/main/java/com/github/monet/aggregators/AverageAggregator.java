/*
 * This class is too trivial to license.
 */
package com.github.monet.aggregators;

import java.util.ArrayList;

/**
 * Aggregates the average of a number of values.
 *
 * @author Max Günther
 *
 */
public class AverageAggregator implements Aggregator<Double> {
	private final ArrayList<Double> values;

	public AverageAggregator() {
		this.values = new ArrayList<Double>();
	}

	@Override
	public void aggregate(Double value) {
		this.values.add(value);
	}

	@Override
	public Double getValue() {
		double sum = 0;
		for (Double d : this.values) {
			sum += d;
		}
		return sum / this.values.size();
	}

}
