package chordest.chord.recognition;

import chordest.chord.Chord;

public interface ITemplateProducer {

	public double[] getTemplateFor(Chord chord);
}
