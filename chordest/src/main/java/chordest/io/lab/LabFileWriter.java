package chordest.io.lab;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import chordest.io.AbstractWriter;
import chordest.model.Chord;


public class LabFileWriter extends AbstractWriter {

	private final Chord[] chords;
	private final double[] timestamps;
	private final boolean concatSame;

	public LabFileWriter(Chord[] chords, double[] timestamps, boolean concat) {
		if (chords == null) {
			throw new NullPointerException("chords is null");
		}
		if (timestamps == null) {
			throw new NullPointerException("timestamps is null");
		}
		if (chords.length + 1 != timestamps.length) {
			throw new IllegalArgumentException(String.format(
					"timestamps.length = %d is not equal to chords.length + 1 = %d",
					timestamps.length, chords.length + 1));
		}
		this.chords = chords;
		this.timestamps = timestamps;
		this.concatSame = concat;
	}

	/**
	 * Creates a new LabFileWriter for given arrays of chords and corresponding
	 * timestamps. Last element of timestamps array must be the total sound 
	 * length in seconds, so that timestamps.length == chords.length + 1
	 * @param chords
	 * @param timestamps
	 */
	public LabFileWriter(Chord[] chords, double[] timestamps) {
		this(chords, timestamps, true);
	}

	@Override
	public void writeTo(Writer writer) throws IOException {
		Chord previous = Chord.empty();
		double start = 0;
		for (int i = 0; i < chords.length; i++) {
			Chord current = chords[i];
			if (current == null) { current = Chord.empty(); }
			if (! current.equals(previous) || ! concatSame) {
				double currentTime = timestamps[i];
				writer.write(getResultLine(start, currentTime, previous));
				previous = current;
				start = currentTime;
			}
		}
		writer.write(getResultLine(start, timestamps[timestamps.length - 1], previous));
	}

	@Override
	public void appendTo(Writer writer) throws IOException {
		writeTo(writer);
	}

	private String getResultLine(double startTime, double endTime, Chord chord) {
		String chordName = chord != null ? chord.toString() : "N";
		return String.format(Locale.ENGLISH, "%f %f %s\n", startTime, endTime, chordName);
	}

}
