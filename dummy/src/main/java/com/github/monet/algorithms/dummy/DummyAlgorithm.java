package com.github.monet.algorithms.dummy;

import java.util.ArrayList;

import org.apache.logging.log4j.Logger;

import com.github.monet.interfaces.Algorithm;
import com.github.monet.interfaces.Meter;
import com.github.monet.worker.Job;
import com.github.monet.worker.ServiceDirectory;

public class DummyAlgorithm implements Algorithm {

	@Override
	public void execute(Job job, Meter meter, ServiceDirectory serviceDir)
			throws Exception {
		Logger log = job.getLogger();
		log.error("Dummy Algorithm on error channel.");
		log.warn("Dummy Algorithm on warning channel.");
		log.info("Dummy Experiment on info channel.");
		log.debug("Dummy Algorithm on debug channel.");
		log.trace("Dummy Algorithm on trace channel.");

		meter.measureInt("measureTest", 6);
		meter.measureDouble("bla/#", 1.0);
		meter.measureDouble("bla/#", 1.0);
		meter.measureDouble("foo/bar", 4.0);
		meter.measureDouble("foo/bar2", 3.0);
		meter.measureDouble("foo/bar3/kabingel", 1337);

		ArrayList<Double> tmp = new ArrayList<Double>();
		tmp.add(5.0);
		tmp.add(7.5);
		meter.measureDouble("foo/bara/double/#", tmp);

		ArrayList<Integer> tmp2 = new ArrayList<Integer>();
		tmp2.add(3);
		tmp2.add(9);
		meter.measureInt("foo/bara/int/#", tmp2);

		ArrayList<Long> tmp3 = new ArrayList<Long>();
		tmp3.add(5L);
		tmp3.add(10L);
		meter.measureLong("foo/bara/long/#", tmp3);

		long[] arr = { 1L, 2L, 3L };
		meter.measureLong("foo/longarr/#", arr);

		// measure pareto front
		double[] point = { 5.0, 2.0 };
		meter.measurePareto(point, null);
		double[] point2 = { 1.5, 3.53 };
		meter.measurePareto(point2, null);

		// enter an endless loop? used for testing cancellation
		boolean loopForever = Boolean.parseBoolean((String) job.getParameters()
				.get("loop_forever"));
		while (loopForever) {
			// keep on looping
		}

		// throw a test exception?
		boolean launch = Boolean.parseBoolean((String) job.getParameters().get(
				"launch_a_nuke"));
		if (launch) {
			Exception e = new Exception("This is a test-Exception");
			StackTraceElement[] st = new StackTraceElement[5];
			for (int i = 0; i <= 4; i++) {
				st[i] = new StackTraceElement("SomeClass", "SomeMethodName",
						"SomeFileName", 42);
			}
			throw e;
		}

		log.error("Dummy Algorithm on error channel.");
		log.warn("Dummy Algorithm on warning channel.");
		log.info("Dummy Experiment on info channel.");
		log.debug("Dummy Algorithm on debug channel.");
		log.trace("Dummy Algorithm on trace channel.");
	}
}
