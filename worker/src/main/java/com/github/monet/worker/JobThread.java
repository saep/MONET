package com.github.monet.worker;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.common.ExceptionUtil;
import com.github.monet.common.ParetoPoint;
import com.github.monet.interfaces.Algorithm;
import com.github.monet.interfaces.GraphParser;
import com.github.monet.interfaces.Meter;
import com.mongodb.DB;

/**
 * A Runnable that executes a single experiment or job in a separate thread.
 *
 * <p>
 * The separation is necessary to keep the rest of the worker, including the
 * OSGi plattform running.
 * </p>
 *
 * @author Johannes Kowald
 *
 */
class JobThread implements Runnable {
	private final static Logger log = LogManager
			.getFormatterLogger(JobThread.class);
	/**
	 * The Experimentor which starts this thread.
	 */
	private final Experimentor experimentor;
	private ServiceDirectory serviceDirectory;

	/**
	 * The corresponding GraphParser implementation.
	 */
	private GraphParser graphParser;

	/**
	 * The corresponding Algorithm implementation.
	 */
	private Algorithm algorithm;

	/**
	 * A new Mongo DB instance instantiated for every job.
	 */
	private DB db;

	/**
	 * Constructor.
	 *
	 * @param experimentor
	 *            the actual Experimentor instance
	 * @param db
	 *            the database to be used in the experiment/job
	 * @param graphParser
	 *            instance of the chosen implementation of the graph parser
	 * @param algorithm
	 *            instance of the chosen algorithm implementation
	 */
	public JobThread(Experimentor experimentor, DB db,
			ServiceDirectory serviceDirectory) {
		this.experimentor = experimentor;
		this.db = db;
		this.algorithm = null;
		this.graphParser = null;
		this.serviceDirectory = serviceDirectory;
	}

	/**
	 * Actually executes a job. This means parsing, creating a meter and calling
	 * the algorithm's execute method.
	 */
	@Override
	public void run() {
		WorkerJob job = this.experimentor.getActiveJob();
		JobState finalState = Job.State.FAILED;
		Meter meter = null;
		boolean outOfMemoryError = false;
		byte[] freeMem = new byte[16 * 1024 * 1024]; // XXX I want RAM...
		try {
			this.algorithm = this.serviceDirectory.getAlgorithm(job
					.getAlgorithmDescriptor());
			this.graphParser = this.serviceDirectory.getGraphParser(job
					.getParserDescriptor());
			job.downloadInputInstance(this.db);
			File inputFile = job.getInputGraphPath();
			job.setState(Job.State.PARSING);
			Object inputGraph = this.graphParser.parse(
					inputFile.getAbsolutePath(), job);
			job.setInputGraph(inputGraph);
			job.setState(Job.State.RUNNING);
			meter = new MongoMeter(this.db, job.getID());
			meter.startExperiment();
			this.algorithm.execute(job, meter,
					this.experimentor.getServiceDirectory());
			meter.endExperiment();
			finalState = Job.State.SUCCESS;
			if ((!job.getState().equals(Job.State.CANCELLED) && !job.getState()
					.equals(Job.State.CANCELLING)) && (meter != null)) {
				List<double[]> front = meter.getParetoFront();
				if (front != null) {
					job.setState(Job.State.CALCULATING_METRICS);
					double metric = ParetoPoint.calculateSMetric(front, true);
					meter.measureDouble("SMetric", metric);
				}
			}
		} catch (KillJobException e) {
			if (meter != null) {
				meter.endExperiment();
			}
			log.info("Job cancelled by Controlserver.");
			finalState = Job.State.CANCELLED;
		} catch (ThreadDeath e) {
			/*
			 * Guess what: You can ignore this error. The statement below is
			 * just to be sure. Also, because of curiosity, a System println. I
			 * think at least one of these exceptions should appear.
			 */
			if (meter != null) {
				meter.endExperiment();
			}
			System.out.println("ThreadDeath catched");
			finalState = Job.State.CANCELLED;
		} catch (StackOverflowError | OutOfMemoryError e) {
			/*
			 * We cannot do much here as allocating any resources would cause
			 * this exception again.
			 */
			algorithm = null;
			freeMem = null;
			System.gc();
			System.out.println(e.getLocalizedMessage());
			e.printStackTrace();
			finalState = abruptTermination(job, meter, e);
			outOfMemoryError = true;
		} catch (Exception e) {
			finalState = abruptTermination(job, meter, e);
		} finally {
			if (!finalState.equals(Job.State.CANCELLED)
					&& !finalState.equals(Job.State.FAILED)) {
				if (meter != null) {
					meter.send();
				}
			}
			this.experimentor.experimentFinished(finalState);
			if (outOfMemoryError) {
				System.exit(12);
			}
		}
	}

	/**
	 * Try to write some stuff to the meter and set the state to failed.
	 *
	 * @param job
	 *            the job
	 * @param meter
	 *            the meter
	 * @param e
	 *            the cause for the error
	 * @return FAILED
	 */
	private JobState abruptTermination(WorkerJob job, Meter meter, Throwable e) {
		if (meter != null) {
			meter.measureString("EXCEPTION", e.getMessage());
			meter.measureString("EXCEPTION-STACKTRACE",
					ExceptionUtil.stacktraceToString(e));
			meter.endExperiment();
		}
		job.getLogger().fatal("job ends with exception", e);
		return Job.State.FAILED;
	}
}
