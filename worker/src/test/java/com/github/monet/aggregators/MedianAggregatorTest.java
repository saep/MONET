package com.github.monet.aggregators;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.github.monet.aggregators.MedianAggregator;

public class MedianAggregatorTest {
	MedianAggregator agg;

	@Before
	public void setUp() {
		this.agg = new MedianAggregator();
	}

	@Test
	public void testAggregateEven() {
		this.agg.aggregate(4.2);
		this.agg.aggregate(0.0);
		this.agg.aggregate(3.7);
		this.agg.aggregate(2.0);
		this.agg.aggregate(90.3);
		this.agg.aggregate(-71.232);
		assertEquals(2.85, this.agg.getValue(), 0.0);
	}

	@Test
	public void testAggregateUneven() {
		this.agg.aggregate(4.2);
		this.agg.aggregate(0.0);
		this.agg.aggregate(2.0);
		this.agg.aggregate(90.3);
		this.agg.aggregate(-71.232);
		assertEquals(2.0, this.agg.getValue(), 0.0);
	}

	@Test
	public void testAggregateZero() {
		this.agg.aggregate(4.2);
		this.agg.aggregate(0.0);
		this.agg.aggregate(2.0);
		assertEquals(2.0, this.agg.getValue(), 0.0);
	}

}
