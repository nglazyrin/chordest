package chordest.chord.comparison;

import java.util.Arrays;

import chordest.model.Chord;
import chordest.model.Note;

public class Tetrads implements IEvaluationMetric {

	@Override
	public Chord map(Chord chord) {
		if (chord.isEmpty() || chord.isOfType(Chord.MAJ) || chord.isOfType(Chord.MIN)) {
			return chord;
		}
		Note root = chord.getRoot();
		String[] shorthands = getOutputTypes();
		for (String shorthand : shorthands) {
			Chord pattern = new Chord(root, shorthand);
			Note[] chordNotes = chord.getNotes();
			Note[] patternNotes = pattern.getNotes();
			if (chordNotes.length < 3) {
				return null;
			}
			if (chordNotes.length < patternNotes.length) {
				continue;
			}
			boolean matches = true;
			for (int i = 0; i < patternNotes.length; i++) {
				if (! chordNotes[i].equals(patternNotes[i])) {
					matches = false;
				}
			}
			if (matches) {
				return pattern;
			}
		}
		return null;
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
		Note[] notes1 = Arrays.copyOf(reference.getNotes(), reference.getNotes().length);
		Note[] notes2 = Arrays.copyOf(estimated.getNotes(), estimated.getNotes().length);
		if (notes1.length != notes2.length) {
			return 0;
		}
		Arrays.sort(notes1);
		Arrays.sort(notes2);
		for (int i = 0; i < notes1.length; i++) {
			if (! notes1[i].equals(notes2[i])) {
				return 0;
			}
		}
		return 1;
	}

	@Override
	public String toString() {
		return "Tetrads";
	}

	@Override
	public String[] getOutputTypes() {
		return new String[] { Chord.DOM, Chord.MAJ7, Chord.MIN7, Chord.MAJ, Chord.MIN };
	}

}
