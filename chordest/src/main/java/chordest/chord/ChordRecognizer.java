package chordest.chord;

import chordest.chord.recognition.IChordRecognition;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.transform.ScaleInfo;

public class ChordRecognizer {

	private static final double MIN_NO_CHORDNESS = 0.0011;

	private final double[][] chroma;
	private final double[] noChordness;
	private final ITemplateProducer producer;

	public ChordRecognizer(double[][] chroma, double[] noChordness, ITemplateProducer producer) {
		this.chroma = chroma;
		this.noChordness = noChordness;
		this.producer = producer;
	}

	public Chord[] recognize(String[] knownChordShorthands) {
		IChordRecognition recognition = new TemplatesRecognition(producer, knownChordShorthands);
		Chord[] temp = recognition.recognize(chroma, new ScaleInfo(1, 12));
//		IChordRecognition recognition = new ExtraTemplatesRecognition(startNote, sp12[0].length); // TODO
//		Chord[] temp = recognition.recognize(sp12, null);
		for (int i = 0; i < noChordness.length; i++) {
			if (noChordness[i] < MIN_NO_CHORDNESS) {
				temp[i] = Chord.empty();
			}
		}
		return Harmony.smoothUsingHarmony(chroma, temp, new ScaleInfo(1, 12), producer);
	}

}
