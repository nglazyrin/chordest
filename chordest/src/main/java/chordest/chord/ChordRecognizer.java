package chordest.chord;

import chordest.chord.recognition.IChordRecognition;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.transform.ScaleInfo;

public class ChordRecognizer {

	private final double[][] chroma;
	private final double[] noChordness;
	private final ITemplateProducer producer;
	private final double noChordnessLimit;

	public ChordRecognizer(double[][] chroma, double[] noChordness,
			ITemplateProducer producer, double noChordnessLimit) {
		this.chroma = chroma;
		this.noChordness = noChordness;
		this.producer = producer;
		this.noChordnessLimit = noChordnessLimit;
	}

	public Chord[] recognize(String[] knownChordShorthands) {
		IChordRecognition recognition = new TemplatesRecognition(producer, knownChordShorthands);
		Chord[] temp = recognition.recognize(chroma, new ScaleInfo(1, 12));
//		IChordRecognition recognition = new ExtraTemplatesRecognition(startNote, sp12[0].length); // TODO
//		Chord[] temp = recognition.recognize(sp12, null);
		for (int i = 0; i < noChordness.length; i++) {
			if (noChordness[i] < noChordnessLimit) {
				temp[i] = Chord.empty();
			}
		}
		return Harmony.smoothUsingHarmony(chroma, temp, new ScaleInfo(1, 12), producer);
	}

}
