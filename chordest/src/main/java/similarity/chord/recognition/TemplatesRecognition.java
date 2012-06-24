package similarity.chord.recognition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import similarity.chord.Chord;
import similarity.chord.Mode;
import similarity.chord.Note;
import similarity.transform.ScaleInfo;
import similarity.util.DataUtil;
import similarity.util.metric.IMetric;
import similarity.util.metric.KLMetric;

public class TemplatesRecognition {

	public static final List<Chord> knownChords;

	private static final Logger LOG = LoggerFactory.getLogger(TemplatesRecognition.class);
	
	public static final IMetric metric = new KLMetric();			// step 4
//	private static final IMetric metric = new EuclideanMetric();

	static {
		String[] shorthands = new String[] {
				Chord.MAJ, Chord.MIN, Chord.AUG, Chord.DIM };
		List<Chord> chords = new ArrayList<Chord>(shorthands.length * Note.values().length);
		for (String shorthand : shorthands) {
			for (Note note : Note.values()) {
				chords.add(new Chord(note, shorthand));
			}
		}
		knownChords = Collections.unmodifiableList(chords);
	}

	public static boolean isKnown(Chord chord) {
		return chord.isEmpty() || knownChords.contains(chord);
	}

	private final Map<Chord, double[]> possibleChords;
	protected final TemplateProducer templateProducer;
	protected final Note startNote;

	/**
	 * All 24 major/minor chords will be used for recognition
	 */
	public TemplatesRecognition(Note pcpStartNote) {
		startNote = pcpStartNote;
		templateProducer = new TemplateProducer(pcpStartNote);
		Map<Chord, double[]> map = getAllChords();
		possibleChords = Collections.unmodifiableMap(map);
	}

	/**
	 * Uses only the chords defined by given <code>mode</code> for recognition
	 * or all known chords if <code>mode</code> is null.
	 * @param mode
	 */
	public TemplatesRecognition(Note pcpStartNote, Mode mode) {
		startNote = pcpStartNote;
		templateProducer = new TemplateProducer(pcpStartNote);
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		if (mode != null) {
			for (Chord chord : mode.getChords()) {
				map.put(chord, templateProducer.getTemplateFor(chord));
			}
		} else {
			map = getAllChords();
		}
		possibleChords = Collections.unmodifiableMap(map);
	}

	private Map<Chord, double[]> getAllChords() {
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		for (Chord chord : knownChords) {
			map.put(chord, templateProducer.getTemplateFor(chord));
		}
		return map;
	}

	public Chord recognize(double[] cqtSpectrum, ScaleInfo scaleInfo) {
		if (cqtSpectrum == null) {
			throw new NullPointerException("spectrum is null");
		}
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		final int notesInOctave = scaleInfo.getNotesInOctaveCount();
		final double[] pcp = DataUtil.toPitchClassProfiles(cqtSpectrum, notesInOctave);
		if (isSmall(pcp)) {
			return Chord.empty();
		}
		final double[] vector = metric.normalize(DataUtil.reduceTo12Notes(pcp));
		
		Map<Chord, Double> distances = new HashMap<Chord, Double>();
		Map<Chord, double[]> chords = possibleChords;
		for (Entry<Chord, double[]> entry : chords.entrySet()) {
			distances.put(entry.getKey(), metric.distance(
					metric.normalize(entry.getValue()), vector));
		}
		
		// find element with minimal distance
		Chord minKey = new Chord();
		double minValue = Double.MAX_VALUE;
		for (Entry<Chord, Double> entry : distances.entrySet()) {
			if (entry.getValue() < minValue) {
				minValue = entry.getValue();
				minKey = entry.getKey();
			}
		}
		return minKey;
	}

	public Chord[] recognize(double[][] cqtSpectrum, ScaleInfo scaleInfo) {
		if (cqtSpectrum == null) {
			throw new NullPointerException("spectrum is null");
		}
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		LOG.debug("Performing recognition...");
		Chord[] result = new Chord[cqtSpectrum.length];
		for (int i = 0; i < cqtSpectrum.length; i++) {
			result[i] = recognize(cqtSpectrum[i], scaleInfo);
		}
		return result;
	}

	private Note getMaxNote(double[] octave) {
		double[] vector = metric.normalize(DataUtil.reduceTo12Notes(octave));
		int maxPos = 0;
		double max = vector[0];
		for (int i = 1; i < vector.length; i++) {
			if (vector[i] > max) {
				maxPos = i;
				max = vector[i];
			}
		}
		return startNote.withOffset(maxPos);
	}

	protected boolean isSmall(double[] vector) {
//		return metric.distance(vector, new double[12]) < 0.05;
		return false;
	}

}
