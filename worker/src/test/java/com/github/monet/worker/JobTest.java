package com.github.monet.worker;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.monet.worker.IllegalStateTransition;
import com.github.monet.worker.Job;
import com.github.monet.worker.JobState;
import com.github.monet.worker.WorkerJob;
import com.github.monet.worker.Job.State;

/**
 * Unit tests for the job class.
 *
 */
public class JobTest {

	@Test
	public void testNew() {
		List<JobState> valid = new LinkedList<>();
		valid.addAll(Arrays.asList(State.ABORTED, State.CANCELLING,
				State.FAILED));
		valid.add(State.CANCELLED);
		valid.add(State.SCHEDULED);
		valid.add(State.NEW);
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.NEW);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.NEW);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.NEW);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testScheduled() {
		List<JobState> valid = new LinkedList<>();
		valid.addAll(Arrays.asList(State.ABORTED, State.CANCELLING,
				State.FAILED));
		valid.add(State.INITIALIZING);
		valid.add(State.SCHEDULED);
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.SCHEDULED);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.SCHEDULED);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.SCHEDULED);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testInitializing() {
		List<JobState> valid = new LinkedList<>();
		valid.addAll(Arrays.asList(State.ABORTED, State.CANCELLING,
				State.FAILED));
		valid.add(State.PARSING);
		valid.add(State.INITIALIZING);
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.INITIALIZING);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.INITIALIZING);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.INITIALIZING);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testParsing() {
		List<JobState> valid = new LinkedList<>();
		valid.addAll(Arrays.asList(State.ABORTED, State.CANCELLING,
				State.FAILED));
		valid.add(State.RUNNING);
		valid.add(State.PARSING);
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.PARSING);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.PARSING);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.PARSING);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testCustom() {
		List<JobState> valid = new LinkedList<>();
		valid.addAll(Arrays.asList(State.ABORTED, State.CANCELLING,
				State.FAILED));
		valid.add(State.SUCCESS);
		valid.add(State.RUNNING);
		valid.add(State.CANCELLED); /* XXX (Johannes) Why is this allowed? */
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.RUNNING);
			j.setState("Test");
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.RUNNING);
			j.setState("Test");
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.RUNNING);
			j.setState("Test");
			j.setState("TestiBus");
			j.setState("Test");
			j.setState(State.RUNNING);
			advanceJobToGivenState(j, State.SUCCESS);
		} catch (IllegalStateTransition e) {
			fail("Cusom states should be reachable from the state RUNNING.");
		}
	}

	@Test
	public void testRunning() {
		List<JobState> valid = new LinkedList<>();
		valid.addAll(Arrays.asList(State.ABORTED, State.CANCELLING,
				State.FAILED));
		valid.add(State.SUCCESS);
		valid.add(State.RUNNING);
		valid.add(State.CANCELLED); /* XXX (Johannes) Why is this allowed? */
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.RUNNING);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.RUNNING);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.RUNNING);
			j.setState("Test");
		} catch (IllegalStateTransition e) {
			fail("Cusom states should be reachable from the state RUNNING.");
		}
	}

	@Test
	public void testSuccess() {
		List<JobState> valid = new LinkedList<>();
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.SUCCESS);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.SUCCESS);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.SUCCESS);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testCancelling() {
		List<JobState> valid = new LinkedList<>();
		valid.add(State.CANCELLED);
		valid.add(State.CANCELLING);
		valid.add(State.ABORTED);	/* XXX (Johannes) Why? */
		valid.add(State.FAILED);	/* XXX (Johannes) Why? */
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.CANCELLING);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.CANCELLING);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.CANCELLING);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testFailed() {
		List<JobState> valid = new LinkedList<>();
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.FAILED);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.FAILED);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.FAILED);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testAborted() {
		List<JobState> valid = new LinkedList<>();
		List<JobState> invalid = new ArrayList<>();
		invalid.addAll(Arrays.asList(State.ABORTED, State.CANCELLED,
				State.CANCELLING, State.FAILED, State.INITIALIZING, State.NEW,
				State.PARSING, State.RUNNING, State.SCHEDULED, State.SUCCESS));
		invalid.removeAll(valid);
		for (JobState s : valid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.ABORTED);
			assertTrue(j.getState().isStateTransitionAllowed(s));
			j.setState(s);
		}
		for (JobState s : invalid) {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.ABORTED);
			assertFalse(j.getState().isStateTransitionAllowed(s));
		}
		try {
			Job j = new WorkerJob(new HashMap<String, Object>());
			advanceJobToGivenState(j, State.ABORTED);
			j.setState("Test");
			fail("Expected an exception!");
		} catch (IllegalStateTransition e) {
			// expected
		}
	}

	@Test
	public void testHashmap() {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		String testAlgorithmDescriptor = "monet.algorithms.DummyAlgorithm";
		String testParserDescriptor = "monet.parser.DummyParser";
		String testGraphDescriptor = "monet.graph.DummyGraph";

		Map<String, Object> testParameters = new HashMap<String, Object>();
		testParameters.put("recursion depth", 1909);
		testParameters.put("path", "path/to/what/ever.txt");
		testParameters.put("array", new int[] { 1, 2, 3 });

		hashmap.put(WorkerJob.KEY_ALGORITHM, testAlgorithmDescriptor);
		hashmap.put(WorkerJob.KEY_GRAPHPARSER, testParserDescriptor);
		hashmap.put(WorkerJob.KEY_GRAPHFILE, testGraphDescriptor);
		hashmap.put(WorkerJob.KEY_PARAMETERS, testParameters);

		WorkerJob job = new WorkerJob(hashmap);
		assertNotNull(job);

		assertEquals(job.getAlgorithmDescriptor(), testAlgorithmDescriptor);
		assertEquals(job.getParserDescriptor(), testParserDescriptor);
		assertEquals(job.getGraphDescriptor(), testGraphDescriptor);

		Map<String, Object> assertParameters = job.getParameters();
		assertNotNull(assertParameters);

		assertEquals((int) assertParameters.get("recursion depth"), 1909);
		assertEquals(assertParameters.get("path"), ("path/to/what/ever.txt"));
		int[] array_paramenter = (int[]) assertParameters.get("array");
		assertEquals(array_paramenter.length, 3);
		assertEquals(array_paramenter[0], 1);
		assertEquals(array_paramenter[1], 2);
		assertEquals(array_paramenter[2], 3);
	}

	@Test
	public void testState() {
		Job job = new WorkerJob(new HashMap<String, Object>());
		assertNotNull(job);

		job.setState(Job.State.NEW);
		assertEquals(Job.State.NEW, job.getState());

		job.setState(Job.State.SCHEDULED);
		assertEquals(Job.State.SCHEDULED, job.getState());

		job.setState(Job.State.INITIALIZING);
		assertEquals(Job.State.INITIALIZING, job.getState());

		job.setState(Job.State.PARSING);
		assertEquals(Job.State.PARSING, job.getState());

		job.setState(Job.State.RUNNING);
		assertEquals(Job.State.RUNNING, job.getState());

		job.setState(Job.State.SUCCESS);
		assertEquals(Job.State.SUCCESS, job.getState());

		try {
			job.setState(Job.State.ABORTED);
			fail("should throw exception");
		} catch (IllegalStateTransition ex) {
			// can't transition from SUCCESS to ABORTED
			assertEquals(Job.State.SUCCESS, job.getState());
		}

		try {
			job.setState(Job.State.FAILED);
			fail("should throw exception");
		} catch (IllegalStateTransition ex) {
			// can't transition from SUCCESS to FAILED
			assertEquals(Job.State.SUCCESS, job.getState());
		}
	}

	private void advanceJobToGivenState(Job j, JobState setTo) {
		if (!(j.getState() instanceof State)) {
			j.setState(State.RUNNING);
			advanceJobToGivenState(j, setTo);
		}
		if (j.getState().isFinal()) {
			return;
		}
		while (!(j.getState().equals(setTo)) && !j.getState().isFinal()) {
			if (j.getState().isStateTransitionAllowed(setTo)) {
				j.setState(setTo);
				break;
			}
			switch ((State) j.getState()) {
			case CANCELLING:
				j.setState(State.CANCELLED);
				break;
			case INITIALIZING:
				j.setState(State.PARSING);
				break;
			case NEW:
				j.setState(State.SCHEDULED);
				break;
			case PARSING:
				j.setState(State.RUNNING);
				break;
			case RUNNING:
				j.setState(State.SUCCESS);
				break;
			case SCHEDULED:
				j.setState(State.INITIALIZING);
				break;
			default:
				break;

			}
		}
	}
}
