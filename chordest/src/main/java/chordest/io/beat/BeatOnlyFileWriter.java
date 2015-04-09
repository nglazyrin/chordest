package chordest.io.beat;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

public class BeatOnlyFileWriter extends BeatFileWriter {

	public BeatOnlyFileWriter(double[] timestamps) {
		super(timestamps);
	}
	
	public static void write(String fileName, double[] timestamps) {
		File file = new File(fileName);
		try {
			FileUtils.forceMkdir(file.getParentFile());
			new BeatOnlyFileWriter(timestamps).writeTo(file);
		} catch (IOException e) {
			LOG.error("Error when saving beat times", e);
		}
		LOG.info("Beats were written to " + fileName);
	}
	
	@Override
	public void writeTo(Writer writer) throws IOException {
		for (int i = 0; i < timestamps.length; i++) {
			writer.write(String.format(Locale.ENGLISH, "%.3f\t", timestamps[i]));
		}
	}
}
