package chordest.chord.templates;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Key;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;


public class TemplatesAndKeyRecognition extends TemplatesRecognition {

	private static final Logger LOG = LoggerFactory.getLogger(TemplatesAndKeyRecognition.class);
	
	private static final int KEY_WINDOW_SIZE = 64;
	
	private ITemplateProducer templateProducer;
	
	public TemplatesAndKeyRecognition(Note pcpStartNote, ITemplateProducer producer) {
		super(pcpStartNote);
		this.templateProducer = producer;
	}

	public Chord[] recognize(double[][] cqtSpectrum, ScaleInfo scaleInfo) {
		if (cqtSpectrum == null) {
			throw new NullPointerException("spectrum is null");
		}
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		Chord[] result = new Chord[cqtSpectrum.length];
		for (int i = 0; i < cqtSpectrum.length; i++) {
			int left = Math.max(0, i - KEY_WINDOW_SIZE / 2);
			int right = Math.min(cqtSpectrum.length, i + KEY_WINDOW_SIZE / 2);
			Key mode = Key.recognizeKey(DataUtil.sumVectors(cqtSpectrum, left, right), startNote);
			result[i] = recognize(cqtSpectrum[i], scaleInfo, mode);
		}
		return result;
	}

	private Chord recognize(double[] cqtSpectrum, ScaleInfo scaleInfo, Key mode) {
		if (cqtSpectrum == null) {
			throw new NullPointerException("spectrum is null");
		}
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		final int notesInOctave = scaleInfo.getNotesInOctaveCount();
		final double[] pcp = DataUtil.toSingleOctave(cqtSpectrum, notesInOctave);
		if (isSmall(pcp)) {
			return Chord.empty();
		}
		final double[] vector = metric.normalize(DataUtil.reduceTo12Notes(pcp));
		
		Map<Chord, Double> distances = new HashMap<Chord, Double>();
		Map<Chord, double[]> chords = new HashMap<Chord, double[]>();
		for (Chord chord : mode.getChords()) {
			chords.put(chord, templateProducer.getTemplateFor(chord));
		}
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

}
