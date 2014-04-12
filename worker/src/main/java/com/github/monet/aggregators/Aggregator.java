/*
 * This interface is too trivial to license.
 */
package com.github.monet.aggregators;

/**
 * An Aggregator aggregates values on the worker side of things.
 *
 * Generally you shouldn't use an Aggregator, but simply record all values and
 * do the aggregation on the control-server side. Using an Aggregator only makes
 * sense if your singular interest is the aggregated value and you might exceed
 * the limit 16MB of measured data per experiment.
 *
 * @author Max Günther
 *
 * @param <T>
 *            the type of what to aggregate
 */
public interface Aggregator<T> {

	/**
	 * Aggregate value.
	 */
	public void aggregate(T value);

	/**
	 * Returns the current value.
	 *
	 * @return the current value
	 */
	public T getValue();

}
