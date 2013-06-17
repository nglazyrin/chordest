package chordest.chord.recognition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.MapUtil;

/**
 * A try to incorporate information about estimated bass note into the process
 * of chord recognition. No success for now, bass note estimation is also
 * unstable, more efforts are needed to combine it with the distance betweeen
 * template and chroma vector.
 * @author Nikolay
 *
 */
public class TemplatesWithBassRecognition extends TemplatesRecognition {

	private final double[][] bass;

	public TemplatesWithBassRecognition(ITemplateProducer producer, double[][] bassChroma) {
		super(producer);
		bass = bassChroma;
	}

	@Override
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
			result[i] = recognize(to12DimensionalFeatureVector(cqtSpectrum[i], scaleInfo), bass[i]);
		}
		return result;
	}

	private Chord recognize(final double[] chormaFeaturesBin, final double[] bassChroma) {
		final double[] vector = metric.normalize(chormaFeaturesBin);
		
		final Map<Chord, Double> distances = new HashMap<Chord, Double>();
		for (Entry<Chord, double[]> entry : possibleChords.entrySet()) {
			double coeff = 0;
			if (! entry.getKey().isEmpty()) {
				int offset = entry.getKey().getRoot().offsetFrom(Note.C);
				coeff = offset < 0 ? bassChroma[offset+12] : bassChroma[offset];
			}
			distances.put(entry.getKey(), metric.distance(metric.normalize(entry.getValue()), vector) + (1 - coeff));
		}
		List<Entry<Chord, Double>> sorted = MapUtil.sortMapByValue(distances, true);
		return sorted.get(0).getKey();
	}

}
