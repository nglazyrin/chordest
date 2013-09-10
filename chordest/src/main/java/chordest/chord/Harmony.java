package chordest.chord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chordest.chord.recognition.AbstractChordRecognition;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.metric.IMetric;

public class Harmony {

	public static Chord[] smoothUsingHarmony(final double[][] pcp, final Chord[] chords, 
			final ScaleInfo scaleInfo, final ITemplateProducer producer) {
		if (pcp == null) {
			throw new NullPointerException();
		}
		if (chords == null) {
			throw new NullPointerException();
		}
		if (pcp.length != chords.length) {
			throw new IllegalArgumentException("pcp length != chords length: "
					+ pcp.length + " != " + chords.length);
		}
		final Chord[] result = new Chord[chords.length];
		for (int i = 0; i < chords.length; i++) {
			result[i] = chords[i];
		}
		
		removeSameRootDifferentType(pcp, result, producer);
		removeSingleBeatChords(pcp, result, producer);
		return result;
	}

	private static void removeSingleBeatChords(final double[][] pcp, Chord[] result,
			ITemplateProducer producer) {
		for (int i = 1; i < result.length - 1; i++) {
			Chord prev = result[i - 1];
			Chord curr = result[i];
			Chord next = result[i + 1];
			if (curr.equals(prev) || curr.equals(next)) {
				// do nothing
			} else if (prev.equals(next)) {
				result[i] = prev;
			} else {
//				final List<Chord> possibleChords = new ArrayList<Chord>(2);
//				possibleChords.add(prev);
//				possibleChords.add(next);
//				final Chord[] top = new TemplatesRecognition(possibleChords,
//						producer).recognize(new double[][] { pcp[i] }, new ScaleInfo(1,12));
//				result[i] = top[0];
				double[] p = producer.getTemplateFor(prev);
				double[] c = producer.getTemplateFor(curr);
				double[] n = producer.getTemplateFor(next);
				double ppn = d(pcp[i-1], p) + d(pcp[i], p) + d(pcp[i+1], n);
				double pnn = d(pcp[i-1], p) + d(pcp[i], n) + d(pcp[i+1], n);
				double ccn = d(pcp[i-1], c) + d(pcp[i], c) + d(pcp[i+1], n);
				double pcc = d(pcp[i-1], p) + d(pcp[i], c) + d(pcp[i+1], c);
				if (ppn < pnn && ppn < ccn && ppn < pcc) {
					result[i] = prev;
				} else if (pnn < ppn && pnn < ccn && pnn < pcc) {
					result[i] = next;
				} else if (ccn < ppn && ccn < pnn && ccn < pcc) {
					result[i-1] = curr;
				} else if (pcc < ppn && pcc < pnn && pcc < ccn) {
					result[i+1] = curr;
				}
			}
		}
	}

	private static double d(double[] v1, double[] v2) {
		IMetric m = AbstractChordRecognition.metric;
		return m.distance(m.normalize(v1), m.normalize(v2));
	}

	private static void removeSameRootDifferentType(final double[][] pcp, final Chord[] result,
			final ITemplateProducer producer) {
		final List<IntervalToCorrect> intervals = gatherIntervals(result);
		for (IntervalToCorrect interval : intervals) {
			final List<Chord> possibleChords = new ArrayList<Chord>(interval.chordTypes.size());
			for (String shortHand : interval.chordTypes) {
				possibleChords.add(new Chord(interval.root, shortHand));
			}
			double[] sum = new double[12];
			for (int i = interval.start; i < interval.end; i++) {
				double[] col = pcp[i];
				sum = DataUtil.add(sum, col);
			}
			final Chord[] top = new TemplatesRecognition(possibleChords,
					producer).recognize(new double[][] { sum }, new ScaleInfo(1,12));
			Chord best = top[0];
			for (int i = interval.start; i < interval.end; i++) {
				result[i] = best;
			}
		}
	}

	private static List<IntervalToCorrect> gatherIntervals(final Chord[] chords) {
		List<IntervalToCorrect> intervals = new ArrayList<IntervalToCorrect>();
		Chord previous = Chord.empty();
		int start = 0;
		int end = -1;
		Set<String> chordTypes = new HashSet<String>();
		for (int i = 0; i < chords.length; i++) {
			Chord chord = chords[i];
			if (chord.equals(previous)) {
				// do nothing
			} else if (chord.hasSameRootDifferentType(previous)) {
				previous = chord;
				chordTypes.add(chord.getShortHand());
			} else {
				end = i;
				IntervalToCorrect interval = getInterval(start, end, previous.getRoot(), chordTypes);
				if (interval != null) {
					intervals.add(interval);
				}
				previous = chord;
				chordTypes = new HashSet<String>();
				chordTypes.add(chord.getShortHand());
				start = i;
			}
		}
		end = chords.length;
		IntervalToCorrect interval = getInterval(start, end, previous.getRoot(), chordTypes);
		if (interval != null) {
			intervals.add(interval);
		}
		return intervals;
	}

	private static IntervalToCorrect getInterval(int start, int end, Note root, Set<String> chordTypes) {
		if (end - start > 1 && chordTypes.size() > 1) {
			return new IntervalToCorrect(start, end, root, chordTypes);
		}
		return null;
	}

	private static class IntervalToCorrect {
		public final int start;
		public final int end;
		public final Note root;
		public final Set<String> chordTypes;
		
		public IntervalToCorrect(int start, int end, Note root, Set<String> types) {
			this.start = start;
			this.end = end;
			this.root = root;
			this.chordTypes = types;
		}
	}

}
