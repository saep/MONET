/*
 * This class is too trivial to license.
 */
package com.github.monet.aggregators;

/**
 * Sums up doubles.
 *
 * @author Max Günther
 *
 */
public class DoubleSumAggregator implements Aggregator<Double> {
	private double sum;

	public DoubleSumAggregator() {
		this.sum = 0.0;
	}

	@Override
	public void aggregate(Double value) {
		this.sum += value;
	}

	@Override
	public Double getValue() {
		return this.sum;
	}

}
