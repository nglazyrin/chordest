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

	@Test
	public void testRootAndMajorWhenNotesInDifferentOrder() {
		Chord c = new Chord(Note.C, Note.E, Note.G);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.C, Note.G, Note.E);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.E, Note.G, Note.C);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.E, Note.C, Note.G);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.G, Note.C, Note.E);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.G, Note.E, Note.C);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
	}

	@Test
	public void testRootAndMinorWhenNotesInDifferentOrder() {
		Chord c = new Chord(Note.C, Note.DD, Note.G);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.C, Note.G, Note.DD);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.DD, Note.G, Note.C);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.DD, Note.C, Note.G);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.G, Note.C, Note.DD);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.G, Note.DD, Note.C);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
	}

	@Test
	public void testRootWhenDoubledNotes() {
		Chord c = new Chord(Note.C, Note.E, Note.G, Note.C);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.C, Note.G, Note.E, Note.E);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.E, Note.G, Note.C, Note.G);
		Assert.assertTrue(c.isMajor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.C, Note.DD, Note.G, Note.C);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.C, Note.G, Note.DD, Note.DD);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
		
		c = new Chord(Note.DD, Note.G, Note.C, Note.G);
		Assert.assertTrue(c.isMinor());
		Assert.assertEquals(Note.C, c.getRoot());
	}

}
