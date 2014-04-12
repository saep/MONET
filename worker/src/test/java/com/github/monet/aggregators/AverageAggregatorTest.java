package com.github.monet.aggregators;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.github.monet.aggregators.AverageAggregator;

public class AverageAggregatorTest {
	AverageAggregator agg;

	@Before
	public void setUp() {
		this.agg = new AverageAggregator();
	}

	@Test
	public void testAggregateEven() {
		this.agg.aggregate(4.2);
		this.agg.aggregate(0.0);
		this.agg.aggregate(3.7);
		this.agg.aggregate(2.0);
		this.agg.aggregate(90.3);
		this.agg.aggregate(-71.232);
		assertEquals(4.828, this.agg.getValue(), 0.0);
	}
}
