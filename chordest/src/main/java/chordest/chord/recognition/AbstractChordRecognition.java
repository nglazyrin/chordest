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

public abstract class AbstractChordRecognition implements IChordRecognition {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractChordRecognition.class);

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
	private double[] to12DimensionalFeatureVector(double[] cqtSpectrumBin, ScaleInfo scaleInfo) {
		if (cqtSpectrumBin == null) {
			throw new NullPointerException("vector is null");
		}
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		final int notesInOctave = scaleInfo.getNotesInOctaveCount();
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
			map.put(chord, templateProducer.getTemplateFor(chord));
		}
		return map;
	}

}
