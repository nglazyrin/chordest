package chordest.chord.recognition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.model.Key;
import chordest.util.MapUtil;

/**
 * This class implements the simplest chord recognition algorithm: a chroma
 * vector is compared to the templates of all known chords, and the nearest
 * chord is selected as the result. Distance is calculated using metric
 * defined in AbstractChordRecognition (Euclidean distance is used now).
 * @author Nikolay
 *
 */
public class TemplatesRecognition extends AbstractChordRecognition {

	public static final List<Chord> knownChords = Chord.getAllChordsWithShorthands(
			new String[] { Chord.MAJ, Chord.MIN, Chord.N });

	public static boolean isKnown(Chord chord) {
		return knownChords.contains(chord);
	}

	protected final Map<Chord, double[]> possibleChords;

	/**
	 * All 24 major/minor chords will be used for recognition
	 */
	public TemplatesRecognition(ITemplateProducer templateProducer) {
		Map<Chord, double[]> map = getTemplatesForChords(templateProducer, knownChords);
		for (Entry<Chord, double[]> entry : map.entrySet()) {
			entry.setValue(metric.normalize(entry.getValue()));
		}
		possibleChords = Collections.unmodifiableMap(map);
	}

	/**
	 * Uses only the chords defined by given <code>key</code> for recognition
	 * or all known chords if <code>key</code> is null.
	 * @param key
	 */
	public TemplatesRecognition(ITemplateProducer templateProducer, Key key) {
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		if (key != null) {
			map = getTemplatesForChords(templateProducer, key.getChords());
		} else {
			map = getTemplatesForChords(templateProducer, knownChords);
		}
		possibleChords = Collections.unmodifiableMap(map);
	}

	public TemplatesRecognition(Collection<Chord> possibleChords, ITemplateProducer producer) {
		Map<Chord, double[]> map = getTemplatesForChords(producer, possibleChords);
		this.possibleChords = Collections.unmodifiableMap(map);
	}

	@Override
	public Chord recognize(final double[] chormaFeaturesBin) {
//		if (isSmall(chormaFeaturesBin)) {
//			return Chord.empty();
//		}
		final double[] vector = metric.normalize(chormaFeaturesBin);
		
		final Map<Chord, Double> distances = new HashMap<Chord, Double>();
		final Map<Chord, double[]> chords = possibleChords;
		for (Entry<Chord, double[]> entry : chords.entrySet()) {
			distances.put(entry.getKey(), metric.distance(entry.getValue(), vector));
		}
		
		// find element with minimal distance
		List<Entry<Chord, Double>> sorted = MapUtil.sortMapByValue(distances, true);
		return sorted.get(0).getKey();
	}

}
