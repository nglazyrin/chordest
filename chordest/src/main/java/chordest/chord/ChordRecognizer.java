package chordest.chord;

import chordest.chord.recognition.IChordRecognition;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.model.Key;
import chordest.transform.ScaleInfo;

public class ChordRecognizer {

	private final BassDetector bassDetector;
	private final double[][] chroma;
	private final double[] noChordness;
	private final ITemplateProducer producer;
	private final double noChordnessLimit;
	private final Key key;

	public ChordRecognizer(double[][] chroma, double[] noChordness,
			ITemplateProducer producer, double noChordnessLimit, Key key) {
		this.bassDetector = null;
		this.chroma = chroma;
		this.noChordness = noChordness;
		this.producer = producer;
		this.noChordnessLimit = noChordnessLimit;
		this.key = key;
	}

	public ChordRecognizer(double[][] chroma, double[] noChordness, BassDetector bassDetector,
			ITemplateProducer producer, double noChordnessLimit, Key key) {
		this.bassDetector = bassDetector;
		this.chroma = chroma;
		this.noChordness = noChordness;
		this.producer = producer;
		this.noChordnessLimit = noChordnessLimit;
		this.key = key;
	}

	public Chord[] recognize(String[] knownChordShorthands) {
		IChordRecognition recognition = new TemplatesRecognition(producer, knownChordShorthands);
		Chord[] temp = recognition.recognize(chroma, new ScaleInfo(1, 12));
		
//		IChordRecognition recognition = new ExtraTemplatesRecognition(startNote, sp12[0].length); // TODO
//		Chord[] temp = recognition.recognize(sp12, null);
		
//		Hmm<ObservationVector> hmm = (Hmm<ObservationVector>) HmmFileReader.read(HmmAdapter.HMM_FILE);
//		Chord[] temp = HmmAdapter.getChordSequence(hmm, chroma);
		
		for (int i = 0; i < noChordness.length; i++) {
			if (noChordness[i] < noChordnessLimit) {
				temp[i] = Chord.empty();
			}
		}
		temp = Harmony.smoothUsingHarmony(chroma, temp, new ScaleInfo(1, 12), producer, key);
//		temp = TactusCorrector.correct(temp);
//		if (bassDetector != null) {
//			temp = bassDetector.detectBass(temp);
//			temp = bassDetector.correctTactus(temp, producer);
//		}
		return temp;
	}

}
