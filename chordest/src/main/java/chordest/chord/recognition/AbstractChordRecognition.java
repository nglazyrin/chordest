package chordest.chord.recognition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.metric.EuclideanMetric;
import chordest.util.metric.IMetric;

/**
 * Abstract class to hold all methods common for different chord recognition
 * strategies.
 * @author Nikolay
 *
 */
public abstract class AbstractChordRecognition implements IChordRecognition {

	protected static final Logger LOG = LoggerFactory.getLogger(AbstractChordRecognition.class);

	public static final IMetric metric = new EuclideanMetric();

	@Override
	public Chord[] recognize(final double[][] cqtSpectrum, final ScaleInfo scaleInfo) {
		LOG.debug("Performing recognition...");
		Chord[] result = new Chord[cqtSpectrum.length];
		for (int i = 0; i < cqtSpectrum.length; i++) {
			result[i] = recognize(to12DimensionalFeatureVector(cqtSpectrum[i], scaleInfo));
		}
		return result;
	}

	/**
	 * Compresses a spectrum bin to 12-dimensional chroma vector
	 * @param cqtSpectrumBin (Possibly) high-dimensional spectrum bin
	 * @param scaleInfo Information about the number of octaves and the number
	 * of "notes" per octave in the given spectrum bin
	 * @return The same bin compressed to 12-dimensions, 1 per pitch class
	 */
	protected double[] to12DimensionalFeatureVector(double[] cqtSpectrumBin, ScaleInfo scaleInfo) {
		final int notesInOctave = scaleInfo.notesInOctave;
		final double[] pcp = DataUtil.toSingleOctave(cqtSpectrumBin, notesInOctave);
		return DataUtil.reduceTo12Notes(pcp);
	}

	/**
	 * Performs actual recognition for a given vector of chroma features
	 * @param chormaFeaturesBin Must be 12-dimensional array
	 * @return The recognized chord
	 */
	protected abstract Chord recognize(double[] chormaFeaturesBin);

	protected Map<Chord, double[]> getTemplatesForChords(ITemplateProducer templateProducer, Collection<Chord> chords) {
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		for (Chord chord : chords) {
			map.put(chord, metric.normalize(templateProducer.getTemplateFor(chord)));
		}
		return map;
	}

}
