package com.github.monet.worker;

import com.github.monet.interfaces.MeasurementStream;

public class TestMeasurementStream implements MeasurementStream {

	@Override
	public void startSection(String name) {
		String content = "\n\n ==================== " + name
				+ " ==================== \n";
		this.write(content);
	}

	@Override
	public void write(String str) {
		System.out.print(str);
	}

	@Override
	public void saveFile() {
		// do nothing
	}

}
