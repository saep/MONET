package com.github.monet.aggregators;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.github.monet.aggregators.IntegerSumAggregator;

public class IntegerSumAggregatorTest {
	IntegerSumAggregator agg;

	@Before
	public void setUp() {
		this.agg = new IntegerSumAggregator();
	}

	@Test
	public void testAggregate() {
		this.agg.aggregate(4);
		this.agg.aggregate(0);
		this.agg.aggregate(3);
		this.agg.aggregate(2);
		this.agg.aggregate(90);
		this.agg.aggregate(-71);
		assertEquals(28, (int) this.agg.getValue());
	}

}
