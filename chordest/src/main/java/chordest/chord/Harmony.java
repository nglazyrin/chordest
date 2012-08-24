package chordest.chord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chordest.chord.recognition.TemplatesRecognition;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;

public class Harmony {

	public static Chord[] smoothUsingHarmony(double[][] pcp, Chord[] chords, 
			ScaleInfo scaleInfo, Note pcpStartNote) {
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
		Chord[] result = new Chord[chords.length];
		for (int i = 0; i < chords.length; i++) {
			result[i] = chords[i];
		}
		
		List<IntervalToCorrect> intervals = gatherIntervals(chords);
		smoothChordSequence(pcp, result, intervals, pcpStartNote);
		return result;
	}

	private static void smoothChordSequence(double[][] pcp, Chord[] result,
			List<IntervalToCorrect> intervals, Note pcpStartNote) {
		for (IntervalToCorrect interval : intervals) {
			List<Chord> possibleChords = new ArrayList<Chord>(interval.chordTypes.size());
			for (String shortHand : interval.chordTypes) {
				possibleChords.add(new Chord(interval.root, shortHand));
			}
			
			double[] sum = new double[12];
			for (int i = interval.start; i < interval.end; i++) {
				double[] col = pcp[i];
				sum = DataUtil.add(sum, col);
			}
			Chord top = new TemplatesRecognition(pcpStartNote, null).recognize(sum, new ScaleInfo(1,12));
			for (int i = interval.start; i < interval.end; i++) {
				result[i] = top;
			}
		}
	}

	private static List<IntervalToCorrect> gatherIntervals(Chord[] chords) {
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
