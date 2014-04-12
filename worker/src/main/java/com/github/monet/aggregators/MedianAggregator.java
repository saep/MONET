/*
 * This class is too trivial to license.
 */
package com.github.monet.aggregators;

import java.util.ArrayList;
import java.util.Collections;

/**
 * "Aggregates" the median value of all values aggregated.
 *
 * @author Max Günther
 *
 */
public class MedianAggregator implements Aggregator<Double> {
	private final ArrayList<Double> values;

	public MedianAggregator() {
		this.values = new ArrayList<Double>();
	}

	@Override
	public void aggregate(Double value) {
		this.values.add(value);
	}

	@Override
	public Double getValue() {
		Collections.sort(this.values);
		int size = this.values.size();
		if (size == 0) {
			return 0.0;
		} else if ((size % 2) == 0) {
			return (this.values.get((size - 1) / 2) + this.values.get(size / 2)) / 2;
		} else {
			return this.values.get(size / 2);
		}
	}

}
