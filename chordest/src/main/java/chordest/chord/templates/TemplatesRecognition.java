package chordest.chord.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Key;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.MapUtil;
import chordest.util.metric.IMetric;
import chordest.util.metric.KLMetric;


public class TemplatesRecognition {

	public static final List<Chord> knownChords;

	private static final Logger LOG = LoggerFactory.getLogger(TemplatesRecognition.class);
	
	public static final IMetric metric = new KLMetric();			// step 4
//	public static final IMetric metric = new CosineMetric();

	private double diff = 0;
	private double vectorsProcessed = 0;

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
	protected final Note startNote;

	/**
	 * All 24 major/minor chords will be used for recognition
	 */
	public TemplatesRecognition(Note pcpStartNote) {
		startNote = pcpStartNote;
		Map<Chord, double[]> map = getAllChords(new TemplateProducer(pcpStartNote, true));
		possibleChords = Collections.unmodifiableMap(map);
	}

	/**
	 * Uses only the chords defined by given <code>key</code> for recognition
	 * or all known chords if <code>key</code> is null.
	 * @param key
	 */
	public TemplatesRecognition(Note pcpStartNote, Key key) {
		startNote = pcpStartNote;
		ITemplateProducer templateProducer = new TemplateProducer(pcpStartNote, true);
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		if (key != null) {
			for (Chord chord : key.getChords()) {
				map.put(chord, templateProducer.getTemplateFor(chord));
			}
		} else {
			map = getAllChords(templateProducer);
		}
		possibleChords = Collections.unmodifiableMap(map);
	}

	public TemplatesRecognition(Note pcpStartNote, Collection<Chord> possibleChords,
			ITemplateProducer producer) {
		startNote = pcpStartNote;
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		for (Chord chord : possibleChords) {
			map.put(chord, producer.getTemplateFor(chord));
		}
		this.possibleChords = Collections.unmodifiableMap(map);
	}

	private Map<Chord, double[]> getAllChords(ITemplateProducer templateProducer) {
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		for (Chord chord : knownChords) {
			map.put(chord, templateProducer.getTemplateFor(chord));
		}
		return map;
	}

	public Chord recognize(final double[] cqtSpectrum, final ScaleInfo scaleInfo) {
		if (cqtSpectrum == null) {
			throw new NullPointerException("spectrum is null");
		}
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		final int notesInOctave = scaleInfo.getNotesInOctaveCount();
		final double[] pcp = DataUtil.toSingleOctave(cqtSpectrum, notesInOctave);
//		if (isSmall(pcp)) {
//			return Chord.empty();
//		}
		final double[] vector = metric.normalize(DataUtil.reduceTo12Notes(pcp));
		
		final Map<Chord, Double> distances = new HashMap<Chord, Double>();
		final Map<Chord, double[]> chords = possibleChords;
		for (Entry<Chord, double[]> entry : chords.entrySet()) {
			distances.put(entry.getKey(), metric.distance(metric.normalize(entry.getValue()), vector));
		}
		
		// find element with minimal distance
		List<Entry<Chord, Double>> sorted = MapUtil.sortMapByValue(distances, true);
		diff += (sorted.get(1).getValue() - sorted.get(0).getValue());
		vectorsProcessed++;
		return sorted.get(0).getKey();
//		Chord minKey = new Chord();
//		double minValue = Double.MAX_VALUE;
//		for (Entry<Chord, Double> entry : distances.entrySet()) {
//			if (entry.getValue() < minValue) {
//				minValue = entry.getValue();
//				minKey = entry.getKey();
//			}
//		}
//		return minKey;
	}

	public Chord[] recognize(final double[][] cqtSpectrum, final ScaleInfo scaleInfo) {
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

	public double getDiffNormalized() {
		return diff / vectorsProcessed;
	}

	protected boolean isSmall(final double[] vector) {
//		return metric.distance(vector, new double[12]) < 0.05;
		return false;
	}

}
