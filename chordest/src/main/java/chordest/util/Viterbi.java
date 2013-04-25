package chordest.util;

import java.util.HashMap;
import java.util.Map;

import chordest.chord.CircleOfFifths;
import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.util.metric.EuclideanMetric;
import chordest.util.metric.IMetric;
import chordest.util.metric.L1Metric;

public class Viterbi {

	public static final int totalChords = 24;

	/**
	 * Transition matrix. [0:11] - major, [12:23] - minor
	 */
	public static final double[][] A = new double[totalChords][totalChords];

	public static final double[] pi = new double[totalChords];

	private static CircleOfFifths cof = new CircleOfFifths();

	static {
		// Init transition matrix with inverted distances on the COF,
		// so that nearest chords get largest numbers
		for (int i = 0; i < totalChords; i++) {
			Chord from = fromState(i);
			for (int j = 0; j < totalChords; j++) {
				Chord to = fromState(j);
//				A[i][j] = 7 - cof.distance(from, to);
				A[i][j] = Math.log(8 - cof.distance(from, to));
			}
			// And normalize each row, so that sum of its elements = 1
			A[i] = new L1Metric().normalize(A[i]);
		}
		
		// Init initial probabilities
		for (int i = 0; i < pi.length; i++) {
			pi[i] = 1.0 / pi.length;
		}
	}

	private static int fromChord(Chord c) {
		if (c.isMajor()) {
			return c.getRoot().ordinal();
		} else if (c.isMinor()) {
			return c.getRoot().ordinal() + 12;
		}
		return -1;
	}

	private static Chord fromState(int i) {
		if (i < 0 || i > totalChords) {
			return null;
		}
		if (i < 12) {
			return Chord.major(Note.byNumber(i));
		} else {
			return Chord.minor(Note.byNumber(i - 12));
		}
	}

	private Map<Chord, double[]> templates = new HashMap<Chord, double[]>();

	private IMetric metric = new EuclideanMetric();

	public Viterbi(ITemplateProducer producer) {
		for (int i = 0; i < totalChords; i++) {
			Chord chord = fromState(i);
			templates.put(chord, metric.normalize(producer.getTemplateFor(chord)));
		}
	}

	public Chord[] decode(double[][] chromas) {
		int[][] psi = new int[chromas.length][totalChords];
		double[] oldDelta = new double[totalChords];
		double[] newDelta = new double[totalChords];
		
		// init delta and psi
		double[] b = getB(chromas[0]);
		for (int i = 0; i < totalChords; i++) {
			oldDelta[i] = pi[i] * b[i];
			psi[0][i] = -1;
		}
		
		// forward
		for (int t = 1; t < chromas.length; t++) {
			b = getB(chromas[t]);
			for (int j = 0; j < totalChords; j++) {
				double[] temp = new double[totalChords];
				double max = -Double.MAX_VALUE;
				int maxPos = -1;
				for (int i = 0; i < totalChords; i++) {
					temp[i] = oldDelta[i] * A[i][j];
					if (temp[i] > max) {
						max = temp[i];
						maxPos = i;
					}
				}
				newDelta[j] = max * b[j];
				psi[t][j] = maxPos;
			}
			
			oldDelta = newDelta;
			newDelta = new double[totalChords];
		}
		
		// backward
		int[] states = new int[chromas.length];
		Chord[] result = new Chord[chromas.length];
		
		double max = -Double.MAX_VALUE;
		int maxPos = -1;
		for (int i = 0; i < totalChords; i++) {
			if (oldDelta[i] > max) {
				max = oldDelta[i];
				maxPos = i;
			}
		}
		
		states[chromas.length - 1] = maxPos;
		result[chromas.length - 1] = fromState(states[chromas.length - 1]);
		for (int t = chromas.length - 2; t >= 0; t--) {
			states[t] = psi[t+1][states[t+1]];
			result[t] = fromState(states[t]);
		}
		return result;
	}

	private double[] getB(double[] chroma) {
		double[] result = new double[totalChords];
		for (int i = 0; i < result.length; i++) {
			double[] from = metric.normalize(chroma);
			double[] to = templates.get(fromState(i));
			result[i] = 10 / (metric.distance(from, to) + 0.00001);
		}
//		return new L1Metric().normalize(result);
		return result;
	}

}
