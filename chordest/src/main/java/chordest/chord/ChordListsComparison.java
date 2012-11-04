package chordest.chord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import chordest.chord.templates.TemplatesRecognition;
import chordest.model.Chord;


public class ChordListsComparison {

	private final Chord[] expected;
	private final double[] expectedTimestamps;
	private final Chord[] actual;
	private final double[] actualTimestamps;
	private final Map<String, Double> errors = new HashMap<String, Double>();
	private double overlapMeasure;
	private double totalSeconds = 0;

	public ChordListsComparison(final Chord[] expected, final double[] expectedTimestamps,
			final Chord[] actual, final double[] actualTimestamps) {
		if (expected == null) {
			throw new NullPointerException("expected is null");
		}
		if (expectedTimestamps == null) {
			throw new NullPointerException("expectedTimestamps is null");
		}
		if (expected.length + 1 != expectedTimestamps.length) {
			throw new IllegalArgumentException(String.format(
					"expectedTimestamps.length = %d is not equal to expected.length + 1 = %d",
					expectedTimestamps.length, expected.length + 1));
		}
		if (actual == null) {
			throw new NullPointerException("actual is null");
		}
		if (actualTimestamps == null) {
			throw new NullPointerException("actualTimestamps is null");
		}
		if (actual.length + 1 != actualTimestamps.length) {
			throw new IllegalArgumentException(String.format(
					"actualTimestamps.length = %d is not equal to actual.length + 1 = %d",
					actualTimestamps.length, actual.length + 1));
		}
		this.expected = expected;
		this.expectedTimestamps = expectedTimestamps;
		this.actual = actual;
		this.actualTimestamps = actualTimestamps;
		
		process();
	}

	private void process() {
//		final double expectedTotalTime = expectedTimestamps[expectedTimestamps.length - 1];
//		final double actualTotalTime = actualTimestamps[actualTimestamps.length - 1];
//		final double totalTime = Math.max(expectedTotalTime, actualTotalTime);
		
		final double[] times = new double[expectedTimestamps.length + actualTimestamps.length];
		System.arraycopy(expectedTimestamps, 0, times, 0, expectedTimestamps.length);
		System.arraycopy(actualTimestamps, 0, times, expectedTimestamps.length, actualTimestamps.length);
		Arrays.sort(times);
		Chord expectedChord = expectedTimestamps[0] == 0 ? expected[0] : Chord.empty();
		Chord actualChord = actualTimestamps[0] == 0 ? actual[0] : Chord.empty();
		double previousTime = 0;
		double result = 0;
		for (double currentTime : times) {
			if (previousTime == currentTime) {
				continue;
			}
//			if (expectedChord.equals(actualChord)) {
			if (TemplatesRecognition.isKnown(expectedChord)) {
				double segmentLength = currentTime - previousTime;
				if (expectedChord.equals(actualChord)) {
//			if (expectedChord.hasCommon3Notes(actualChord) || (Chord.empty().equals(expectedChord) && Chord.empty().equals(actualChord))) {
					result += segmentLength;
				} else {
					String key = expectedChord.toString() + "-" + actualChord.toString();
					double value = segmentLength;
					if (errors.containsKey(key)) {
						value += errors.get(key);
					}
					errors.put(key, value);
				}
				totalSeconds += segmentLength;
			}
			int expectedPos = Arrays.binarySearch(expectedTimestamps, currentTime);
			int actualPos = Arrays.binarySearch(actualTimestamps, currentTime);
			if (expectedPos >= 0) {
				if (expectedPos < expectedTimestamps.length - 1) {
					expectedChord = expected[expectedPos];
				} else {
					expectedChord = Chord.empty();
				}
			}
			if (actualPos >= 0) {
				if (actualPos < actualTimestamps.length - 1) {
					actualChord = actual[actualPos];
				} else {
					actualChord = Chord.empty();
				}
			}
			previousTime = currentTime;
		}
		this.overlapMeasure = result / totalSeconds;
	}

	public double getOverlapMeasure() {
		return this.overlapMeasure;
	}

	public Map<String, Double> getErrors() {
		return this.errors;
	}

	public double getTotalSeconds() {
		return this.totalSeconds;
	}

}
