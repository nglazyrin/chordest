package chordest.chord.recognition;

import java.util.Map;

import chordest.chord.Chord;

public class SimpleTemplateProducer implements ITemplateProducer {

	private final Map<Chord, double[]> templates;

	public SimpleTemplateProducer(Map<Chord, double[]> templates) {
		this.templates = templates;
	}

	@Override
	public double[] getTemplateFor(Chord chord) {
		return templates.get(chord);
	}

}
