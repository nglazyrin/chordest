package chordest.chord.tonnetz;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.util.MapUtil;
import chordest.util.metric.EuclideanMetric;
import chordest.util.metric.IMetric;

public class PureTonnetzRecognition {

	private static final Logger LOG = LoggerFactory.getLogger(PureTonnetzRecognition.class);
	private static final IMetric metric = new EuclideanMetric();

	private final Map<Chord, double[]> possibleChords;

	public static void main(String[] args) {
		PureTonnetzRecognition p = new PureTonnetzRecognition(Note.C);
		for (Entry<Chord, double[]> e : p.possibleChords.entrySet()) {
			LOG.info(e.getKey() + " " + Arrays.toString(e.getValue()));
		}
	}

	public PureTonnetzRecognition(Note pcpStartNote) {
		Map<Chord, double[]> map = getTemplatesForChords(new TemplateProducer(pcpStartNote, false),
				Chord.getAllChordsWithShorthands(new String[] { Chord.MAJ, Chord.MIN, Chord.AUG, Chord.DIM }));
		for (Chord chord : map.keySet()) {
			map.put(chord, toTonalCentroid(map.get(chord)));
		}
		possibleChords = Collections.unmodifiableMap(map);
	}

	private Map<Chord, double[]> getTemplatesForChords(ITemplateProducer templateProducer, Collection<Chord> chords) {
		Map<Chord, double[]> map = new HashMap<Chord, double[]>();
		for (Chord chord : chords) {
			map.put(chord, templateProducer.getTemplateFor(chord));
		}
		return map;
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
				result[i] += TonnetzRecognition.PHI[i][j] * chromaVector[j];
			}
		}
		return metric.normalize(result);
	}

	public Chord[] recognize(final double[][] spectrum) {
		if (spectrum == null) {
			throw new NullPointerException("spectrum is null");
		}
		LOG.debug("Performing recognition...");
		Chord[] result = new Chord[spectrum.length];
		for (int i = 0; i < spectrum.length; i++) {
			result[i] = recognize(spectrum[i]);
		}
		return result;
	}

	protected Chord recognize(double[] spectrum) {
		double[] tonnetz = metric.normalize(spectrum);
//		double[] tonnetz = spectrum;
		final Map<Chord, Double> distances = new HashMap<Chord, Double>();
		final Map<Chord, double[]> chords = possibleChords;
		for (Entry<Chord, double[]> entry : chords.entrySet()) {
			distances.put(entry.getKey(), metric.distance(entry.getValue(), tonnetz));
		}
		
		// find element with minimal distance
		List<Entry<Chord, Double>> sorted = MapUtil.sortMapByValue(distances, true);
		return sorted.get(0).getKey();
	}

}
