package chordest.io.csv;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import chordest.chord.ChordListsComparison;
import chordest.io.AbstractWriter;
import chordest.model.Chord;


public class CsvFileWriter extends AbstractWriter {

	private final Chord[] chords;
	private final double[] timestamps;

	/**
	 * Creates a new LabFileWriter for given arrays of chords and corresponding
	 * timestamps. Last element of timestamps array must be the total sound 
	 * length in seconds, so that timestamps.length == chords.length + 1
	 * @param chords
	 * @param timestamps
	 */
	public CsvFileWriter(Chord[] chords, double[] timestamps) {
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
	}

	@Override
	public void writeTo(Writer writer) throws IOException {
		writer.write("start,end,chord,known\n");
		appendTo(writer);
	}

	@Override
	public void appendTo(Writer writer) throws IOException {
		Chord previous = Chord.empty();
		double start = 0;
		for (int i = 0; i < chords.length; i++) {
			Chord current = chords[i];
			if (current == null) { current = Chord.empty(); }
			if (! current.equals(previous)) {
				double currentTime = timestamps[i];
				writer.write(getResultLine(start, currentTime, previous));
				previous = current;
				start = currentTime;
			}
		}
		writer.write(getResultLine(start, this.timestamps[chords.length], previous));
	}

	private String getResultLine(double startTime, double endTime, Chord chord) {
		String chordName = chord != null ? chord.toString() : "N";
		chordName = chordName.replace(',', '-');
		boolean isKnown = ChordListsComparison.isKnown(chord);
		return String.format(Locale.ENGLISH, "%f,%f,%s,%s\n", startTime, endTime, chordName, isKnown);
	}

}
