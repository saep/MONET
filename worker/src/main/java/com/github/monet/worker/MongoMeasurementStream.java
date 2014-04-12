package com.github.monet.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import com.github.monet.common.DBCollections;
import com.github.monet.interfaces.MeasurementStream;
import com.github.monet.interfaces.Meter;
import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

/**
 * A file stream that can be used to recored arbitrary data during the execution
 * of an algorithm. It's obtained through {@link Meter#getMeasurementStream()}.
 * Note that this method of recording measured values should only be used if you
 * think you are going to exceed 16MB of recorded data per run of one algorithm.
 *
 * <p>
 * You may use any kind of file format, but there are some methods here to write
 * data and split the file into sections. If you use these methods the
 * analysator is capable of recognizing the sections and interpreting the ones
 * that have a special meaning defined for them (like <code>PARETOFRONT</code>
 * and <code>GRAPH</code>).
 * </p>
 *
 * @author Max Günther
 *
 */
public class MongoMeasurementStream implements MeasurementStream {
	private GridFS files;
	private String jobID;
	private File tmpFile;
	private FileWriter tmpWriter;

	MongoMeasurementStream(DB db, String jobID) throws IOException {
		this.tmpFile = File.createTempFile(
				String.format("experiment-%s", jobID),
				"mongoMeasurementStream");
		this.tmpWriter = new FileWriter(this.tmpFile);
		this.jobID = jobID;
		this.files = new GridFS(db, DBCollections.MEASUREMENT_FILES);
	}

	@Override
	public void startSection(String name) {
		String content = "\n\n ==================== " + name
				+ " ==================== \n";
		this.write(content);
	}

	@Override
	public void write(String str) {
		try {
			this.tmpWriter.write(str);
		} catch (IOException e) {
			// TODO log
			e.printStackTrace();
		}
	}

	@Override
	public void saveFile() {
		try {
			this.tmpWriter.flush();
			this.tmpWriter.close();
			InputStream in = new FileInputStream(this.tmpFile);
			GridFSInputFile gridFile = this.files.createFile(in,
					this.jobID);
			gridFile.save();
			in.close();
		} catch (IOException e) {
			// TODO log
			e.printStackTrace();
		}
	}

}
