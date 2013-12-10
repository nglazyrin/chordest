package chordest.chord.comparison;

import java.util.List;

import chordest.model.Chord;
import chordest.model.Note;

public class Mirex2010 implements IEvaluationMetric {

	@Override
	public Chord map(Chord chord) {
		return chord;
	}

	@Override
	public double score(Chord reference, Chord estimated) {
		if (reference == null) {
			throw new NullPointerException("reference chord is null");
		}
		if (estimated == null) {
			throw new NullPointerException("estimated chord is null");
		}
		if (reference.isEmpty() && estimated.isEmpty()) {
			return 1;
		}
		if (reference.isOfType(Chord.AUG) || reference.isOfType(Chord.DIM)) {
			if (commonNotes(reference, estimated) >= 2) {
				return 1;
			}
			return 0;
		}
		if (commonNotes(reference, estimated) >= 3) {
			return 1;
		}
		return 0;
	}

	private int commonNotes(Chord reference, Chord estimated) {
		List<Note> refNotes = reference.getNotesAsList();
		int common = 0;
		for (Note note : estimated.getNotes()) {
			if (refNotes.contains(note)) {
				common++;
			}
		}
		return common;
	}

	@Override
	public String toString() {
		return "Mirex2010";
	}

}
