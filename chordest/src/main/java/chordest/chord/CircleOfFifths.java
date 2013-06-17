package chordest.chord;

import chordest.model.Chord;
import chordest.model.Interval;
import chordest.model.Note;

/**
 * A utility class just to calculate the distance between two chords as the
 * number of steps in the shortest path between them on the circle of fifths.
 * @author Nikolay
 *
 */
public class CircleOfFifths {

	public Chord getRelativeMajor(Chord chord) {
		if (chord.isMinor()) {
			return Chord.major(chord.getRoot().withOffset(Interval.MINOR_THIRD));
		} else if (chord.isMajor()) {
			return chord;
		}
		return null;
	}

	public Chord getRelativeMinor(Chord chord) {
		if (chord.isMajor()) {
			return Chord.minor(chord.getRoot().withOffset(Interval.MAJOR_SIXTH));
		} else if (chord.isMinor()) {
			return chord;
		}
		return null;
	}

	public Note getNextRoot(Note note) {
		return note.withOffset(Interval.PERFECT_FIFTH);
	}

	public int distance(Chord c1, Chord c2) {
		if (! (c1.isMajor() || c1.isMinor())) {
			throw new IllegalArgumentException("c1 must be major or minor: " + c1);
		}
		if (! (c2.isMajor() || c2.isMinor())) {
			throw new IllegalArgumentException("c2 must be major or minor: " + c2);
		}
		int typeDif = 0;
		if (c1.isMajor() && c2.isMinor()) {
			typeDif = 1;
			c2 = getRelativeMajor(c2);
		} else if (c1.isMinor() && c2.isMajor()) {
			typeDif = 1;
			c1 = getRelativeMajor(c1);
		}
		Note c1root = c1.getRoot();
		Note c2root = c2.getRoot();
		int d = 0;
		while (! c2root.equals(c1root)) {
			c2root = getNextRoot(c2root);
			d++;
		}
		d = Math.min(d, 12 - d);
		return d + typeDif;
	}

}
