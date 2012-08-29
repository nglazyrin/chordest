package chordest.lab;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;

import chordest.chord.Chord;
import chordest.chord.ChordExtractor;


public class LabFileWriter extends AbstractWriter {

	private final Chord[] chords;
	private final double[] timestamps;

	/**
	 * Creates a new LabFileWriter for given arrays of chords and corresponding
	 * timestamps. Last element of timestamps array must be the total sound 
	 * length in seconds, so that timestamps.length == chords.length + 1
	 * @param chords
	 * @param timestamps
	 */
	public LabFileWriter(Chord[] chords, double[] timestamps) {
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

	/**
	 * Appends one more timestamp that marks end of the last chord played.
	 * The segment between that timestamp and the end of the file is treated as
	 * containing N (no chord).
	 * @param ce
	 */
	public LabFileWriter(ChordExtractor ce) {
		if (ce == null) {
			throw new NullPointerException("Chord extractor is null");
		}
		if (ce.getChords().length + 1 != ce.getOriginalBeatTimes().length) {
			throw new IllegalArgumentException(String.format(
					"timestamps.length = %d is not equal to chords.length + 1 = %d",
					ce.getOriginalBeatTimes().length, ce.getChords().length + 1));
		}
		chords = Arrays.copyOf(ce.getChords(), ce.getChords().length + 1);
		chords[chords.length - 1] = Chord.empty();
		double[] beatTimes = Arrays.copyOf(ce.getOriginalBeatTimes(), ce.getOriginalBeatTimes().length + 1);
		if (beatTimes.length > 2) {
			double beatLength = beatTimes[1] - beatTimes[0];
			double lastSound = beatTimes[beatTimes.length - 3] + beatLength;
			beatTimes[beatTimes.length - 1] = beatTimes[beatTimes.length - 2];
			beatTimes[beatTimes.length - 2] = lastSound;
		}
		this.timestamps = beatTimes;
	}

	@Override
	public void writeTo(Writer writer) throws IOException {
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
