package com.github.monet.algorithms.dummy;

import com.github.monet.interfaces.GraphParser;
import com.github.monet.worker.Job;

public class DummyParser implements GraphParser {

    @Override
    public Object parse(String inputFile, Job job) {
	job.getLogger().info("parsing nothing and returning null.");
	return null;
    }

}
