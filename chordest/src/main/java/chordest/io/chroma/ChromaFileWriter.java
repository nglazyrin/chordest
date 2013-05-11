package chordest.io.chroma;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.AbstractWriter;

public class ChromaFileWriter extends AbstractWriter {

	private static final Logger LOG = LoggerFactory.getLogger(ChromaFileWriter.class);

	private final double[] timestamps;
	
	private final double[][] chroma;

	public static void write(String fileName, double[][] chroma, double[] timestamps) {
		File file = new File(fileName);
		try {
			FileUtils.forceMkdir(file.getParentFile());
			new ChromaFileWriter(chroma, timestamps).writeTo(file);
		} catch (IOException e) {
			LOG.error("Error when saving chroma", e);
		}
		LOG.info("Timestamps and chroma were written to " + fileName);
	}

	public ChromaFileWriter(double[][] chroma, double[] timestamps) {
		if (chroma.length != timestamps.length) {
			throw new IllegalArgumentException(
					"chroma.length = " + chroma.length + " != timestamps.length = " + timestamps.length);
		}
		this.chroma = chroma;
		this.timestamps = timestamps;
	}

	@Override
	public void writeTo(Writer writer) throws IOException {
		for (int i = 0; i < timestamps.length; i++) {
			writer.write(getResultLine(timestamps[i], chroma[i]));
		}
	}

	@Override
	public void appendTo(Writer writer) throws IOException {
		writeTo(writer);
	}

	private String getResultLine(double timestamp, double[] chromaBin) {
		StringBuilder sb = new StringBuilder();
		sb.append(timestamp);
		sb.append(": ");
		for (int i = 0; i < chromaBin.length; i++) {
			sb.append(chromaBin[i]);
			sb.append(" ");
		}
		sb.append("\n");
		return sb.toString();
	}

}
