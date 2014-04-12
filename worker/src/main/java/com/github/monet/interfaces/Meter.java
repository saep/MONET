/*
 * Copyright (C) 2013,2014
 * Jakob Bossek, Michael Capelle, Hendrik Fichtenberger, Max Günther, Johannes
 * Kowald, Marco Kuhnke, David Mezlaf, Christopher Morris, Andreas Pauly, Sven
 * Selmke and Sebastian Witte
 *
 *  This interface is part of MONET.
 *
 *  This interface is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This interface is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with MONET.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.monet.interfaces;

import java.util.Collection;
import java.util.List;

import com.github.monet.aggregators.Aggregator;
import com.github.monet.worker.Experimentor;

/**
 * This interface is used to measure and record values during the execution of
 * an algorithm using an arbitrary backend (such as mongo db or a test backend).
 *
 * All these methods (like {@link #measureInt(String, int)}) use a String
 * parameter that specifies a path where the second parameter (the value) is to
 * be saved. These path expressions are build like regular file path
 * expressions, but don't actually correspond to files on a filesystem, but to
 * entries in a JSON document. Since all the expressions are absolute, the "/"
 * at the first position can be omitted. For lists or arrays there is a special
 * path element: "<code>#n</code>", where <code>n</code> is the index of value
 * in the list. <code>n</code> can be omitted in order to append a value to the
 * list. Indices start with 0. </p> Example:
 *
 * <pre>
 * <code>
 * mesaureInt("someint", 4711);
 * measureInt("mykey/foo/bar", 1);
 * measureInt("mykey/foo/bar2", 2);
 * measureDouble("bla/#", 0.1);
 * measureDouble("bla/#", 0.2);
 * measureDouble("bla/#1", 0.3);
 * measureString("bla/#/dumm", "dumm");
 * measureString("bla/#/dumm2", "dumm2");
 * measureString("bla/#3/dumm2", "dumm2 overwritten");
 * measureString("bla/#3/muh", "second item");
 * </code>
 * </pre>
 *
 * <h2>Conventions and reserved paths</h2> In general all keys in all CAPS have
 * some kind of special purpose. This usually means the are interpreted by the
 * gui or the analysator in some way.
 * <ul>
 * <li><code>RUNTIME</code> - runtime of the algorithm</li>
 * <li><code>RAM</code> - memory size in MiB</li>
 * <li><code>CPU</code> - "cxg" where c=number of cores and g=tactrate in Mhz</li>
 * <li><code>CPU_NAME</code> - "CPU_NAME" - name of the cpu
 * (e.g."AMD Phenom(tm) II X4 965 Processor × 4") "OS" - Name des OS (z.B.
 * "Windows XP", "Ubuntu 13.04")</li>
 * <li><code>KERNEL</code> - version of the Linux kernel * (e.g. "3.8.0-19")</li>
 * <li><code>MONET_VERSION</code> - version of the MONET platform (e.g. "1.0")</li>
 * <li><code>ALGORITHM</code> - name of the executed algorithm (e.g. "max-NSP")</li>
 * <li><code>VERSION</code> - version of the executed algorithm</li>
 * <li><code>DATETIME</code> - the date and time when the execution of the
 * algorithm began (e.g. "2013/04/30 21:55:23")</li>
 * <li><code>OVERFULL</code> - <code>true</code> if the measured data (that is
 * the genereated JSON document) exceeded the threshold of 16MB,
 * <code>false</code> otherwise</li>
 * <li><code>PROBLEM_HASH</code> - SHA256-Hash of the problem instance, to be
 * used for easy identification</li>
 * <li><code>CONFIG_HASH</code> - SHA256-Hash of the parameter map,
 * <code>ALGORITHM</code> and <code>VERSION</code>. This way the configuration
 * in which an algorithm was launched (except for input) can be easily
 * identified.</li>
 * <li><code>PLATFORM_HASH</code> - SHA256-Hash of <code>RAM</code>,
 * <code>CPU</code>, <code>CPU_NAME</code>, <code>OS</code>, <code>KERNEL</code>, <code>MONET_VERSION</code></li>
 * <li><code>EXCEPTION</code> - usually <code>null</code> or the message of an
 * exception encountered during the execution of an algorithm</li>
 * </ul>
 *
 *
 * Some other keys need to be supplied by the implementor of an algorithm, but
 * have a special meaning:
 * <ul>
 * <li><code>RESULT</code> - result of the algorithm. Typically this is a
 * paretofront: <code>RESULT/#/1</code>, value of the first objective,
 * <code>RESULT/#/2</code>, value of the second objective, etc.</li>
 * </ul>
 *
 * @author Max Günther
 * @see MongoMeter, TestMeter
 *
 */
public interface Meter {

	/**
	 * Start an experiment.
	 *
	 * Is called by the {@link Experimentor} and doesn't have to be called by an
	 * implementation of {@link Algorithm}.
	 *
	 * @param uuid
	 *            the UUID identifying the job/experiment
	 * @param db
	 *            the database to be used
	 * @see #endExperiment()
	 */
	public abstract void startExperiment();

	/**
	 * Finish the active experiment.
	 *
	 * Is called by the {@link JobThread} and doesn't have to be called by an
	 * implementation of {@link Algorithm}.
	 *
	 * @see #startExperiment()
	 */
	public abstract void endExperiment();

	/**
	 * Send the measured data to a database. Optional.
	 *
	 * Is called by the {@link JobThread} and doesn't have to be called by an
	 * implementation of {@link Algorithm}.
	 *
	 * @see #startExperiment()
	 */
	public abstract void send();

	/**
	 * Add an aggregator to be used with a path. Whenever you measure using the
	 * same path, the aggregator will be used. Note that you should you use the
	 * right measure type to match up with the type of the aggregator.
	 *
	 * @param path
	 * @param agg
	 * @see Aggregator
	 */
	public abstract void addAggregator(String path, Aggregator<Object> agg);

	/**
	 * Saves the measured integer value at the given <code>path</code>.
	 *
	 * @param path
	 *            where the value is to be saved
	 * @param value
	 *            an integer value to be measured
	 */
	public abstract void measureInt(String path, int value);

	/**
	 * Measure an array of integers.
	 *
	 * @param path
	 *            where the integers are to be saved
	 * @param arr
	 *            the array of integers to be saved
	 */
	public abstract void measureInt(String path, int[] arr);

	/**
	 * Convenient methods to measure iterable objects
	 *
	 * @param path
	 *            where the value is to be saved
	 * @param val
	 *            an iterable object of integer values to be saved
	 */
	public abstract void measureInt(String path, Iterable<Integer> val);

	/**
	 * Saves the measured long value at the given <code>path</code>.
	 *
	 * @param path
	 *            where the value is to be saved
	 * @param value
	 *            a long value to be measured
	 */
	public abstract void measureLong(String path, long value);

	/**
	 * Convenient methods to measure iterable objects
	 *
	 * @param path
	 *            where the value is to be saved
	 * @param val
	 *            an iterable object of long values to be saved
	 */
	public abstract void measureLong(String path, Iterable<Long> val);

	/**
	 * Measure an array of longs.
	 *
	 * @param path
	 *            where the array of longs is to be saved
	 * @param values
	 *            an array of the longs to be saved
	 */
	public abstract void measureLong(String path, long[] values);

	/**
	 * Saves the measured double value at the given <code>path</code>.
	 *
	 * @param path
	 *            where the value is to be saved
	 * @param value
	 *            an integer value to be measured
	 */
	public abstract void measureDouble(String path, double value);

	/**
	 * Convenient methods to measure iterable objects
	 *
	 * @param path
	 *            where the value is to be saved
	 * @param val
	 *            an iterable object of double values to be saved
	 */
	public abstract void measureDouble(String path, Iterable<Double> val);

	/**
	 * Measure an array of doubles.
	 *
	 * @param path
	 *            where the values are to be saved
	 * @param arr
	 *            the arry of doubles to be measured
	 */
	public abstract void measureDouble(String path, double[] arr);

	/**
	 * Saves the measured String value at the given <code>path</code>.
	 *
	 * @param path
	 *            where the value is to be saved
	 * @param value
	 *            a String value to be measured
	 */
	public abstract void measureString(String path, String value);

	/**
	 * Measure a single value on a pareto front. Note that no checks are made
	 * whether the point dominates any other measured points.
	 *
	 * @param point
	 *            array of dimensions of a single point in the pareto front
	 * @param edges
	 *            collection of edges that are included in that solution
	 *            resulting in the point on the pareto front or null, if
	 *            measuring this isn't relevant
	 *
	 */
	public abstract void measurePareto(double[] point, Collection<String> edges);

	/**
	 * Returns the pareto front that has been measured.
	 *
	 * @return the measured pareto front or null, if none has been measured
	 */
	public abstract List<double[]> getParetoFront();

	/**
	 * Starts a new timer at the given <code>path</code>. The measured elapsed
	 * time is saved when the {@link #stopTimer(String)}-method is called.
	 *
	 * @param path
	 *            used to identify a pair of startTimer and stopTimer and for
	 *            the save location
	 * @see #stopTimer(String)
	 */
	public abstract void startTimer(String path);

	/**
	 * Stops the timer at the given <code>path</code> and records the elapsed
	 * time.
	 *
	 * If there was not corresponding call of {@link #startTimer(String)}
	 * nothing happens.
	 *
	 * @param path
	 *            identifies the timer to be stopped and specifies the save
	 *            location of the elapsed time
	 */
	public abstract void stopTimer(String path);

	/**
	 * Returns an open {@link MeasurementStream} to which any data can be
	 * written in any format.
	 *
	 * The resulting file is written to MongoDB's GridFS and can generally not
	 * be interpreted by the analysator. The only advantage of using this method
	 * to record values is that MongoDB's limit of 16GB per document can be
	 * exceeded.
	 *
	 * Note that while the format of the file is open there are some methods for
	 * formatting implemented in the MeasurementStream.
	 *
	 * @return a stream ready to be written to
	 */
	public abstract MeasurementStream getMeasurementStream();

}
