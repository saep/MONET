package com.github.monet.algorithms.dummy;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.github.monet.worker.ServiceDirectory;

public class DummyActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("starting dummy algorithm bundle");
		ServiceDirectory.registerAlgorithm(context, new DummyAlgorithm());
		ServiceDirectory.registerGraphParser(context, new DummyParser());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
