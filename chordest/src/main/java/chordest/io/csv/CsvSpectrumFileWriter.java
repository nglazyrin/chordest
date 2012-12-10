package chordest.io.csv;

import java.io.IOException;
import java.io.Writer;

import chordest.io.AbstractWriter;

public class CsvSpectrumFileWriter extends AbstractWriter {

	private final double[][] data;

	public CsvSpectrumFileWriter(double[][] data) {
		if (data == null) {
			throw new NullPointerException("data is null");
		}
		this.data = data;
	}

	@Override
	public void writeTo(Writer writer) throws IOException {
		appendTo(writer);
	}

	@Override
	public void appendTo(Writer writer) throws IOException {
		for (int i = 0; i < data.length; i++) {
			String line = toCommaSeparatedString(data[i]);
			writer.write(line);
		}
	}

	private String toCommaSeparatedString(double[] line) {
		StringBuilder sb = new StringBuilder();
		if (line != null && line.length > 0) {
			for (int j = 0; j < line.length - 1; j++) {
				sb.append(line[j]);
				sb.append(',');
			}
			sb.append(line[line.length - 1]);
			sb.append("\r\n");
		}
		return sb.toString();
	}

}
