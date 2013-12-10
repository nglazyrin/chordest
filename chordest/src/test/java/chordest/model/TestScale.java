package chordest.model;

import org.junit.Assert;
import org.junit.Test;

import chordest.model.Scale.NaturalMajor;
import chordest.model.Scale.NaturalMinor;

public class TestScale {

	@Test
	public void testNaturalMajor() {
		Chord[] chords = new NaturalMajor().getChords(Note.C);
		Assert.assertArrayEquals(new Chord[] {
				Chord.major(Note.C),
				Chord.minor(Note.D),
				Chord.minor(Note.E),
				Chord.major(Note.F),
				Chord.major(Note.G),
				Chord.minor(Note.A)
		}, chords);
	}

	@Test
	public void testNaturalMinor() {
		Chord[] chords = new NaturalMinor().getChords(Note.A);
		Assert.assertArrayEquals(new Chord[] {
				Chord.minor(Note.A),
				Chord.major(Note.C),
				Chord.minor(Note.D),
				Chord.minor(Note.E),
				Chord.major(Note.F),
				Chord.major(Note.G)
		}, chords);
	}

}
