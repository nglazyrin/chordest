package chordest.chord;

import junit.framework.Assert;

import org.junit.Test;

import chordest.model.Chord;
import chordest.model.Note;

public class CircleOfFifthsTest {

	private CircleOfFifths cof = new CircleOfFifths();

	@Test
	public void testRelativeMajor() {
		Assert.assertEquals(Chord.major(Note.A), cof.getRelativeMajor(Chord.minor(Note.FD)));
		Assert.assertEquals(Chord.major(Note.CD), cof.getRelativeMajor(Chord.minor(Note.AD)));
		
		Assert.assertEquals(Chord.major(Note.CD), cof.getRelativeMajor(Chord.major(Note.CD)));
		
		Assert.assertNull(cof.getRelativeMajor(Chord.empty()));
	}

	@Test
	public void testRelativeMinor() {
		Assert.assertEquals(Chord.minor(Note.DD), cof.getRelativeMinor(Chord.major(Note.FD)));
		Assert.assertEquals(Chord.minor(Note.G), cof.getRelativeMinor(Chord.major(Note.AD)));
		
		Assert.assertEquals(Chord.minor(Note.G), cof.getRelativeMinor(Chord.minor(Note.G)));
		
		Assert.assertNull(cof.getRelativeMinor(Chord.empty()));
	}

	@Test
	public void testGetNextRoot() {
		Assert.assertEquals(Note.G, cof.getNextRoot(Note.C));
		Assert.assertEquals(Note.C, cof.getNextRoot(Note.F));
		Assert.assertEquals(Note.GD, cof.getNextRoot(Note.CD));
		Assert.assertEquals(Note.AD, cof.getNextRoot(Note.DD));
	}

	@Test
	public void testDistance() {
		Assert.assertEquals(1, cof.distance(Chord.major(Note.C), Chord.major(Note.G)));
		Assert.assertEquals(1, cof.distance(Chord.major(Note.F), Chord.major(Note.C)));
		Assert.assertEquals(1, cof.distance(Chord.minor(Note.C), Chord.minor(Note.G)));
		Assert.assertEquals(1, cof.distance(Chord.minor(Note.F), Chord.minor(Note.C)));
		
		Assert.assertEquals(0, cof.distance(Chord.major(Note.F), Chord.major(Note.F)));
		Assert.assertEquals(0, cof.distance(Chord.minor(Note.F), Chord.minor(Note.F)));
		
		Assert.assertEquals(1, cof.distance(Chord.major(Note.F), Chord.minor(Note.D)));
		Assert.assertEquals(1, cof.distance(Chord.minor(Note.D), Chord.major(Note.F)));
		
		Assert.assertEquals(4, cof.distance(Chord.major(Note.D), Chord.minor(Note.D)));
		Assert.assertEquals(4, cof.distance(Chord.minor(Note.D), Chord.major(Note.D)));
		
		Assert.assertEquals(7, cof.distance(Chord.major(Note.C), Chord.minor(Note.DD)));
		Assert.assertEquals(7, cof.distance(Chord.minor(Note.F), Chord.major(Note.D)));
		
		Assert.assertEquals(3, cof.distance(Chord.minor(Note.F), Chord.major(Note.AD)));
		Assert.assertEquals(3, cof.distance(Chord.major(Note.AD), Chord.minor(Note.F)));
	}

}
