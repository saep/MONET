/*
 * This class is too trivial to license.
 */
package com.github.monet.aggregators;

/**
 * Sums up integers.
 *
 * @author Max Günther.
 *
 */
public class IntegerSumAggregator implements Aggregator<Integer> {
	private Integer sum;

	public IntegerSumAggregator() {
		this.sum = 0;
	}

	@Override
	public void aggregate(Integer value) {
		this.sum = this.sum + value;
	}

	@Override
	public Integer getValue() {
		return this.sum;
	}

}
