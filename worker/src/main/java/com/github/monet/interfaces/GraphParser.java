/*
 * This interface is too trivial to license.
 */
package com.github.monet.interfaces;

import com.github.monet.worker.Job;

/**
 * Due to the variety of graph types and representations there is a need for
 * miscellaneous graph parsers that can convert the specific input file into the
 * wanted graph type. Those parsers must implement a common interface that
 * provides the {@link GraphParser#parse(String, Job)} method.
 */
public interface GraphParser {

	/**
	 * Parses a given input file considering the parameters of the {@link Job}
	 * and transforms it into an Object that represents the graph.
	 *
	 * @param inputFile
	 *            A representation of the graph as a {@linkplain String}
	 * @param job
	 *            The associated {@link Job}
	 * @return A representation of the graph as an {@linkplain Object}
	 */
	public Object parse(String inputFile, Job job);

}
