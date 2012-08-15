package chordest.beat;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.lab.AbstractWriter;


public class BeatFileWriter extends AbstractWriter {

	private static final Logger LOG = LoggerFactory.getLogger(BeatFileWriter.class);

	private final double[] timestamps;

	public static void write(String fileName, double[] timestamps) {
		File file = new File(fileName);
		try {
			FileUtils.forceMkdir(file.getParentFile());
			new BeatFileWriter(timestamps).writeTo(file);
		} catch (IOException e) {
			LOG.error("Error when saving beat times", e);
		}
		LOG.info("Beats were written to " + fileName);
	}

	public BeatFileWriter(double[] timestamps) {
		if (timestamps == null) {
			throw new NullPointerException("timestamps is null");
		}
		this.timestamps = timestamps;
	}

	@Override
	public void writeTo(Writer writer) throws IOException {
		for (int i = 0; i < timestamps.length; i++) {
			writer.write(getResultLine(timestamps[i]));
		}
	}

	@Override
	public void appendTo(Writer writer) throws IOException {
		writeTo(writer);
	}

	private String getResultLine(double time) {
		return String.format(Locale.ENGLISH, "%f\n", time);
	}

}
