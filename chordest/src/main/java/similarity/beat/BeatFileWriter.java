package similarity.beat;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import similarity.lab.AbstractWriter;

public class BeatFileWriter extends AbstractWriter {

	private final double[] timestamps;

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
