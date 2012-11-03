package chordest.chord;

import junit.framework.Assert;

import org.junit.Test;

import chordest.model.Chord;
import chordest.model.Note;

public class ChordTest {

	@Test
	public void testEqualsShorthandAndNotes() {
		Chord c1 = Chord.major(Note.C);
		Chord c2 = new Chord(Note.E, Note.G, Note.C);
		Assert.assertTrue(c1.equals(c2));
		Assert.assertTrue(c2.equals(c1));
	}

	@Test
	public void testEqualsWhenNoteIsDoubled() {
		Chord c1 = new Chord(Note.E, Note.G, Note.C);
		Chord c2 = new Chord(Note.C, Note.E, Note.G, Note.C);
		Assert.assertTrue(c1.equals(c2));
		Assert.assertTrue(c2.equals(c1));
	}

	@Test
	public void testAugmentedChordsAreSame() {
		Chord c1 = new Chord(Note.C, Chord.AUG);
		Chord c2 = new Chord(Note.E, Chord.AUG);
		Assert.assertTrue(c1.equals(c2));
		Assert.assertTrue(c2.equals(c1));
	}

	@Test
	public void testSameRootDifferentType() {
		Chord c1 = Chord.major(Note.D);
		Chord c2 = Chord.minor(Note.D);
		Assert.assertTrue(c1.hasSameRootDifferentType(c2));
		Assert.assertTrue(c2.hasSameRootDifferentType(c1));
		Assert.assertFalse(c1.hasSameRootDifferentType(c1));
		
		c2 = new Chord(Note.D, Chord.AUG);
		Assert.assertTrue(c1.hasSameRootDifferentType(c2));
		Assert.assertTrue(c2.hasSameRootDifferentType(c1));
		
		c2 = Chord.minor(Note.E);
		Assert.assertFalse(c1.hasSameRootDifferentType(c2));
		Assert.assertFalse(c2.hasSameRootDifferentType(c1));
	}

}
