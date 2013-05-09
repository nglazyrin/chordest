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

	@Test
	public void testEqualsToTriadMajor() {
		Chord c1 = new Chord(Note.DD, Chord.MAJ);
		Chord c2 = Chord.major(Note.DD);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.DD, Chord.MAJ7);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.DD, Chord.DOM);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.DD, Chord.MAJ6);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.DD, Chord.NON);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.DD, Chord.MAJ9);
		Assert.assertTrue(c1.equalsToTriad(c2));
	}

	@Test
	public void testEqualsToTriadMinor() {
		Chord c1 = new Chord(Note.G, Chord.MIN);
		Chord c2 = Chord.minor(Note.G);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.G, Chord.MIN7);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.G, Chord.MINMAJ7);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.G, Chord.MIN6);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.G, Chord.MIN9);
		Assert.assertTrue(c1.equalsToTriad(c2));
	}

	@Test
	public void testEqualsToTriadNoChord() {
		Chord c1 = Chord.empty();
		Chord c2 = Chord.empty();
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = Chord.major(Note.C);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c1 = Chord.empty();
		c2 = Chord.major(Note.C);
		Assert.assertFalse(c1.equalsToTriad(c2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotEqualsToTriadThrowsExceptionWhenNotMajMinTriad() {
		Chord c1 = Chord.major(Note.C);
		Chord c2 = new Chord(Note.C, Chord.DOM);
		c1.equalsToTriad(c2);
	}

	@Test
	public void testNotEqualsToTriad() {
		Chord c1 = Chord.major(Note.E);
		Chord c2 = Chord.major(Note.C);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c2 = Chord.major(Note.C);
		
		c1 = new Chord(Note.C, Chord.AUG);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Chord.DIM);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Chord.SUS2);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Chord.SUS4);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c2 = Chord.minor(Note.C);
		
		c1 = new Chord(Note.C, Chord.AUG);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Chord.DIM);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Chord.SUS2);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Chord.SUS4);
		Assert.assertFalse(c1.equalsToTriad(c2));
	}

	@Test
	public void testEqualsToTriadIntervals() {
		Chord c1 = new Chord(Note.C, Note.E, Note.G, Note.A);
		Chord c2 = Chord.major(Note.C);
		c1.equalsToTriad(c2);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Note.DD, Note.G, Note.A);
		Assert.assertFalse(c1.equalsToTriad(c2));
		
		c2 = Chord.minor(Note.C);
		Assert.assertTrue(c1.equalsToTriad(c2));
		
		c1 = new Chord(Note.C, Note.G);
		c2 = Chord.major(Note.C);
		Assert.assertFalse(c1.equalsToTriad(c2));
	}

}
