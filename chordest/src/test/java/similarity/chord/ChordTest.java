package similarity.chord;

import junit.framework.Assert;

import org.junit.Test;

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

}
