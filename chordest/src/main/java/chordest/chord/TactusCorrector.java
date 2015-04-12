package chordest.chord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.metric.EuclideanMetric;

public class TactusCorrector {

	public static Chord[] correct(final Chord[] chords, final double[][] spectrum,
			final ScaleInfo scaleInfo, final ITemplateProducer producer) {
		Chord[] result = Arrays.copyOf(chords, chords.length);
		int[] changePositions = findChordChangePositions(chords);
		int[] durations = getDurations(changePositions);
		boolean ok = false;
		while (! ok) {
			boolean isChanged = false;
			for (int i = 0; i < durations.length - 2; i++) {
				// ignore the last segment, it may have any duration
				Chord c1 = chords[changePositions[i + 1] - 1];
				Chord c2 = chords[changePositions[i + 1]];
				double[] s1 = spectrum[changePositions[i + 1] - 1];
				double[] s2 = spectrum[changePositions[i + 1]];
				if ((durations[i] % 2 == 1 && durations[i + 1] % 2 == 1)) {
					if (durations[i + 1] > 1 && needToShiftRight(c1, c2, s1, s2, scaleInfo.octaves, producer)) {
						durations[i] += 1;
						durations[i + 1] -= 1;
						result[changePositions[i + 1]] = result[changePositions[i + 1] - 1];
						isChanged = true;
						break;
					} else if (durations[i] > 1 && !needToShiftRight(c1, c2, s1, s2, scaleInfo.octaves, producer)) {
						durations[i] -= 1;
						durations[i + 1] += 1;
						result[changePositions[i + 1] - 1] = result[changePositions[i + 1]];
						isChanged = true;
						break;
					}
				}
			}
			ok = !isChanged;
		}
		return result;
	}

	private static int[] findChordChangePositions(final Chord[] chords) {
		List<Integer> changePositions = new ArrayList<Integer>();
		Chord previous = null;
		for (int i = 0; i < chords.length; i++) {
			if (chords[i] != previous) {
				changePositions.add(i);
				previous = chords[i];
			}
		}
		changePositions.add(chords.length);
		int[] result = new int[changePositions.size()];
		for (int i = 0; i < changePositions.size(); i++) {
			result[i] = changePositions.get(i);
		}
		return result;
	}

	private static int[] getDurations(int[] changePositions) {
		int[] result = new int[changePositions.length - 1];
		for (int i = 0; i < changePositions.length - 1; i++) {
			result[i] = changePositions[i + 1] - changePositions[i];
		}
		return result;
	}

	private static boolean needToShiftRight(Chord c1, Chord c2, double[] s1,
			double[] s2, int octaves, ITemplateProducer producer) {
		double[] chroma1 = DataUtil.reduce(s1, octaves);
		chroma1 = DataUtil.toSingleOctave(chroma1, 12);
		double[] chroma2 = DataUtil.reduce(s2, octaves);
		chroma2 = DataUtil.toSingleOctave(chroma2, 12);
		double[] t1 = producer.getTemplateFor(c1);
		double[] t2 = producer.getTemplateFor(c2);
		double d11 = new EuclideanMetric().distance(chroma1, t1);
		double d12 = new EuclideanMetric().distance(chroma1, t2);
		double d21 = new EuclideanMetric().distance(chroma2, t1);
		double d22 = new EuclideanMetric().distance(chroma2, t2);
		return d11 + d21 < d12 + d22;
	}

}
