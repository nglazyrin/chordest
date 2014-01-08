package chordest.chord.recognition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;

/**
 * This class implements the simplest chord recognition algorithm: a chroma
 * vector is compared to the templates of all known chords, and the nearest
 * chord is selected as the result. Distance is calculated using metric
 * defined in AbstractChordRecognition (Euclidean distance is used now).
 * @author Nikolay
 *
 */
public class TemplatesRecognition extends AbstractChordRecognition {

	protected final Map<Chord, double[]> possibleChords;

	public TemplatesRecognition(ITemplateProducer templateProducer, String[] knownChords) {
		if (! ArrayUtils.contains(knownChords, Chord.N)) {
			knownChords = ArrayUtils.add(knownChords, Chord.N);
		}
		Map<Chord, double[]> map = getTemplatesForChords(templateProducer, Chord.getAllChordsWithShorthands(knownChords));
		possibleChords = Collections.unmodifiableMap(map);
	}

	public TemplatesRecognition(ITemplateProducer templateProducer, Collection<Chord> possibleChords) {
		Map<Chord, double[]> map = getTemplatesForChords(templateProducer, possibleChords);
		Set<String> knownChords = new HashSet<String>();
		for (Chord chord : possibleChords) {
			knownChords.add(chord.getShortHand());
		}
		this.possibleChords = Collections.unmodifiableMap(map);
	}

	@Override
	public Chord recognize(final double[] chormaFeaturesBin) {
		final double[] vector = metric.normalize(chormaFeaturesBin);
		
		double minDistance = Double.MAX_VALUE;
		Chord result = null;
		for (Entry<Chord, double[]> entry : possibleChords.entrySet()) {
			double d = metric.distance(entry.getValue(), vector);
			if (d < minDistance) {
				minDistance = d;
				result = entry.getKey();
			}
		}
		return result;
	}

}
