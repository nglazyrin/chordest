package chordest.chord.templates;

import java.util.Map;

import chordest.model.Chord;

/**
 * This class is used only for tests. Only returns a chord template if it was
 * given in the map argument to the constructor.
 * @author Nikolay
 *
 */
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
