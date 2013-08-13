package chordest.chord.comparison;

import chordest.model.Chord;

public interface IEvaluationMetric {

	public Chord map(Chord chord);
	public double score(Chord reference, Chord estimated);
}
