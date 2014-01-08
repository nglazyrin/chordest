package chordest.chord.tonnetz;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import chordest.chord.recognition.AbstractChordRecognition;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration.TemplateProperties;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.util.MapUtil;
import chordest.util.metric.L1Metric;

/**
 * As opposed to other recognition strategies, deals with 6-dimensional tonnetz
 * (or tonal centroid) vectors. Chroma vectors are first converted to tonnetz,
 * then the Euclidean distance in the 6-dimensional space is calculated.
 * @author Nikolay
 *
 */
public class TonnetzRecognition extends AbstractChordRecognition {

	public static final double R1 = 1;
	public static final double R2 = 1;
	public static final double R3 = 0.5;
	public static final double[][] PHI = new double[6][];

	private static final L1Metric L1 = new L1Metric();

	public final Map<Chord, double[]> possibleChords;

	static {
		for (int i = 0; i < 6; i++) {
			PHI[i] = new double[12];
		}
		
		for (int i = 0; i < 12; i++) {
			PHI[0][i] = R1 * Math.sin(i * 7 * Math.PI / 6);
			PHI[1][i] = R1 * Math.cos(i * 7 * Math.PI / 6);
			PHI[2][i] = R2 * Math.sin(i * 3 * Math.PI / 2);
			PHI[3][i] = R2 * Math.cos(i * 3 * Math.PI / 2);
			PHI[4][i] = R3 * Math.sin(i * 2 * Math.PI / 3);
			PHI[5][i] = R3 * Math.cos(i * 2 * Math.PI / 3);
		}
	}

	/**
	 * All 24 major/minor chords will be used for recognition
	 */
	public TonnetzRecognition(Note pcpStartNote, TemplateProperties p) {
		Map<Chord, double[]> map = getTemplatesForChords(new TemplateProducer(pcpStartNote, p),
				Chord.getAllChordsWithShorthands(new String[] { Chord.MAJ, Chord.MIN, Chord.AUG, Chord.DIM }));
		for (Chord chord : map.keySet()) {
			map.put(chord, toTonalCentroid(map.get(chord)));
		}
		possibleChords = Collections.unmodifiableMap(map);
	}

	@Override
	protected Chord recognize(double[] chromaVector) {
		double[] tonalCentroid = toTonalCentroid(chromaVector);
		
		final Map<Chord, Double> distances = new HashMap<Chord, Double>();
		final Map<Chord, double[]> chords = possibleChords;
		for (Entry<Chord, double[]> entry : chords.entrySet()) {
			distances.put(entry.getKey(), metric.distance(entry.getValue(), tonalCentroid));
		}
		
		// find element with minimal distance
		List<Entry<Chord, Double>> sorted = MapUtil.sortMapByValue(distances, true);
		return sorted.get(0).getKey();
	}

	/**
	 * 
	 * @param chromaVector
	 * @return PHI * chromaVector
	 */
	private double[] toTonalCentroid(double[] chromaVector) {
//		chromaVector = metric.normalize(chromaVector);
		double[] result = new double[6];
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 12; j++) {
				result[i] += PHI[i][j] * chromaVector[j];
			}
		}
		return L1.normalize(result);
	}

}
