package chordest.chord.comparison;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import chordest.model.Chord;

/**
 * To be used together with ChordListsComparison to accumulate statistics over
 * a number of tracks. Returns aggregated metrics and all errors sorted by
 * total time in descending orger.
 * @author Nikolay
 *
 */
public class ComparisonAccumulator {
	
	private static String[] types = new String[] {
			Chord.MAJ, Chord.MIN, Chord.DOM, Chord.MAJ7, Chord.MIN7 };

	private double totalOverlap = 0;
	private double totalWeightedOverlap = 0;
	private double totalSegmentation = 0;
	private int totalTracks = 0;
	private double totalEffectiveLength = 0;
	private double totalLength = 0;
	private double totalErrorLength = 0;
	private final Map<Pair<Chord, Chord>, Double> errors = new HashMap<Pair<Chord, Chord>, Double>();

	public void append(ChordListsComparison cmp) {
		final double overlap = cmp.getOverlapMeasure();
		final double effectiveSeconds = cmp.getEffectiveSeconds();
		totalOverlap += overlap;
		totalWeightedOverlap += (overlap * effectiveSeconds);
		totalSegmentation += cmp.getSegmentation();
		totalEffectiveLength += effectiveSeconds;
		totalLength += cmp.getTotalSeconds();
		totalTracks++;
		
		Map<Pair<Chord, Chord>, Double> errorsCurrent = cmp.getErrors();
		for (Entry<Pair<Chord, Chord>, Double> entry : errorsCurrent.entrySet()) {
			Pair<Chord, Chord> key = entry.getKey();
			Double value = entry.getValue();
			totalErrorLength += value;
			if (errors.containsKey(key)) {
				value += errors.get(key);
			}
			errors.put(key, value);
		}
	}

	private double getAOR() {
		return totalOverlap / totalTracks;
	}

	private double getWAOR() {
		return totalWeightedOverlap / totalEffectiveLength;
	}

	private double getAverageSegm() {
		return totalSegmentation / totalTracks;
	}

	private Errors getAllErrors() {
		return new Errors(errors);
	}
	
	public void printStatistics(Logger logger) {
		logger.info("Average chord overlap ratio: " + getAOR());
		logger.info("Weighted average chord overlap ratio: " + getWAOR());
		logger.info("Average segmentation: " + getAverageSegm());
	}
	
	public void printErrorStatistics(Logger logger) {
		Errors err = getAllErrors();
		logger.info("Total length of tracks: " + totalLength + " s");
		logger.info("Effective length: " + totalEffectiveLength + " s");
		logger.info("Total erroneous segments length: " + totalErrorLength + " s");
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < types.length; j++) {
				printTypes(logger, types[i], types[j], err.times[i][j], ArrayUtils.subarray(err.all[i], j*12, (j+1)*12));
			}
		}
		logger.info("N instead of chord: " + err.chordN + " s");
		logger.info("Chord instead of N: " + err.nChord + " s");
		logger.info("Others: " + err.others + " s");
	}
	
	private void printTypes(Logger logger, String expectedType, String actualType, double time, double[] values) {
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		if (sum > 0) {
			logger.info(String.format("%s - %s: %f.2 s %s", expectedType, actualType, time, Arrays.toString(values)));
		}
	}
	
	public static class Errors {
		private static final int TOTAL_CHORDS = 60;
		public double[][] all = new double[types.length][TOTAL_CHORDS];
		public double[][] times = new double[types.length][types.length];

		public double nChord;
		
		public double chordN;

		public double others;
		
		public Errors(Map<Pair<Chord, Chord>, Double> log) {
			for (Entry<Pair<Chord, Chord>, Double> entry : log.entrySet()) {
				double time = entry.getValue();
				Chord expected = entry.getKey().getLeft();
				Chord actual = entry.getKey().getRight();
				int index = getIndex(expected, actual);
				boolean processed = false;
				if (expected.isEmpty()) {
					nChord += time;
					processed = true;
				} else if (actual.isEmpty()) {
					chordN += time;
					processed = true;
				} else if (! isMirexChord(expected)) {
					others += time;
					processed = true;
				} else {
					for (int i = 0; i < types.length && ! processed; i++) {
						if (expected.isOfType(types[i])) {
							all[i][index] += time;
							times[i][index / 12] += time;
							processed = true;
						}
					}
				}
				if (! processed) {
					others += time;
				}
			}
		}

		private boolean isMirexChord(Chord chord) {
			for (int i = 0; i < types.length; i++) {
				if (chord.isOfType(types[i])) {
					return true;
				}
			}
			return false;
		}
		
		private int getIndex(Chord expected, Chord actual) {
			if (expected == null || expected.isEmpty()) {
				return -1;
			}
			if (isMirexChord(actual)) {
				int diff = (expected.getRoot().ordinal() - actual.getRoot().ordinal() + 12) % 12;
				for (int i = 0; i < types.length; i++) {
					if (actual.isOfType(types[i])) {
						return diff + 12 * i;
					}
				}
			}
			return -1;
		}
	}
}
