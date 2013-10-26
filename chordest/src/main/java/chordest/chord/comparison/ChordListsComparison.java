package chordest.chord.comparison;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import chordest.model.Chord;

/**
 * An in-home solution to calculate chord overlap and segmentation. The code
 * is not based on MIREX code, so there is no guarantee the results will be 
 * the same. They were at least similar, though. Also MIREX chord recognition
 * metrics have been changing from year to year, see 
 * http://recherche.ircam.fr/anasyn/peeters/ARTICLES/Pauwels_2013_ICASSP_EvalChord.pdf .
 * 
 * The current version of code maps all chords from ground truth that start
 * from major third + minor third or minor third + major third to major and
 * minor chords correspondingly. Other chords are discarded and do not
 * contribute to the result.
 * 
 * @author Nikolay
 *
 */
public class ChordListsComparison {

	public static final IEvaluationMetric metric = new Mirex2010();
//	public static final IEvaluationMetric metric = new Tetrads();
//	public static final IEvaluationMetric metric = new Triads();
	
	private final Chord[] expected;
	private final double[] expectedTimestamps;
	private final Chord[] actual;
	private final double[] actualTimestamps;
	private final Map<Pair<Chord, Chord>, Double> errors = new HashMap<Pair<Chord, Chord>, Double>();
	private double overlapMeasure;
	private double segmentation;
	private double effectiveSeconds = 0;
	private double totalSeconds;

	/**
	 * Determines whether this chord from the ground truth will contribute to
	 * the result or not. Unknown chords are not considered during the
	 * calculation of metrics.
	 * @param chord
	 * @return
	 */
	public static boolean isKnown(Chord chord) {
		return metric.map(chord) != null;
	}

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
		calculateSegmentation();
	}

	/**
	 * See Mathias Mauch's PhD, p. 52 (2010) for details.
	 */
	private void calculateSegmentation() {
		double ea = getSegmentation(expectedTimestamps, actualTimestamps);
		double ae = getSegmentation(actualTimestamps, expectedTimestamps);
		totalSeconds = Math.max(expectedTimestamps[expectedTimestamps.length - 1], actualTimestamps[actualTimestamps.length - 1]);
		segmentation = 1 - Math.max(ea, ae) / totalSeconds;
	}

	private double getSegmentation(double[] from, double[] to) {
		int toIdx = 0;
		int maxIntersectIdx = 0;
		double value = 0;
		for (int i = 0; i < from.length - 1; i++) {
			double start = from[i];
			double end = from[i+1];
			double maxIntersect = 0;
			toIdx = maxIntersectIdx;
			while (toIdx < to.length-1 && to[toIdx] <= end) {
				double intersect = getIntersect(start, end, to[toIdx], to[toIdx + 1]);
				if (intersect > maxIntersect) {
					maxIntersect = intersect;
					maxIntersectIdx = toIdx;
				}
				toIdx++;
			}
			value += (end - start - maxIntersect);
		}
		return value;
	}

	private double getIntersect(double start, double end, double t1, double t2) {
		if (t1 > t2 || start > end) {
			throw new IllegalArgumentException("Start > end:" + start + ", " + end + ", " + t1 + ", " + t2);
		}
		if (t2 < start || t1 > end) {
			return 0;
		}
		if (t1 >= start && t2 <= end) {
			return t2 - t1;
		}
		if (t1 <= start && t2 <= end) {
			return t2 - start;
		}
		if (t1 >= start && t2 >= end) {
			return end - t1;
		}
		if (t1 <= start && t2 >= end) {
			return end - start;
		}
		throw new IllegalArgumentException("Start > end:" + start + ", " + end + ", " + t1 + ", " + t2);
	}

	private void process() {
		// merge 2 lists of timestamps
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
			if (isKnown(expectedChord)) {
				double segmentLength = currentTime - previousTime;
				double score = metric.score(metric.map(expectedChord), metric.map(actualChord));
				result += (score * segmentLength);
				if (score == 0) {
					Pair<Chord, Chord> key = new ImmutablePair<Chord, Chord>(expectedChord, actualChord);
					double value = segmentLength;
					if (errors.containsKey(key)) {
						value += errors.get(key);
					}
					errors.put(key, value);
				}
				effectiveSeconds += segmentLength;
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
		this.overlapMeasure = result / effectiveSeconds;
	}

	public double getOverlapMeasure() {
		return overlapMeasure;
	}

	public Map<Pair<Chord, Chord>, Double> getErrors() {
		return errors;
	}

	public double getSegmentation() {
		return segmentation;
	}

	public double getEffectiveSeconds() {
		return effectiveSeconds;
	}

	public double getTotalSeconds() {
		return totalSeconds;
	}

}
