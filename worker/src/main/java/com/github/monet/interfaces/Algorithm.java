/*
 * This interface is too trivial to license.
 */
package com.github.monet.interfaces;

import com.github.monet.worker.Job;
import com.github.monet.worker.ServiceDirectory;

/**
 * Interface for algorithms which are executable by the Monet tool.
 */
public interface Algorithm {

	/**
	 * Executes the algorithm with the parameters of the given {@link Job}. Also
	 * takes the {@link Meter} and {@link ServiceDirectory} to work with. Throws
	 * an Exception in case of unexpected behavior.
	 *
	 * @param job
	 * @param meter
	 * @param serviceDir
	 * @throws Exception
	 */
	public abstract void execute(Job job, Meter meter,
			ServiceDirectory serviceDir) throws Exception;

}
