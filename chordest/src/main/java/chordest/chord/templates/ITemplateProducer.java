package chordest.chord.templates;

import chordest.model.Chord;

public interface ITemplateProducer {

	public double[] getTemplateFor(Chord chord);
}
