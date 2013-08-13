package chordest.chord.comparison;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import chordest.model.Chord;
import chordest.model.Note;

public class Mirex2010Test {

	private static double E = 1e-5;

	private Mirex2010 metric;

	@Before
	public void setUp() {
		metric = new Mirex2010();
	}

	@Test
	public void testNoChords() {
		Assert.assertEquals(1, metric.score(Chord.empty(), Chord.empty()), E);
	}

	@Test
	public void testAug() {
		Assert.assertEquals(1, metric.score(new Chord(Note.C, Chord.AUG), new Chord(Note.E, Chord.AUG)), E);
		Assert.assertEquals(1, metric.score(new Chord(Note.C, Chord.AUG), Chord.major(Note.C)), E);
	}

	@Test
	public void testDim() {
		Assert.assertEquals(1, metric.score(new Chord(Note.C, Chord.DIM), Chord.minor(Note.C)), E);
	}

	@Test
	public void testTriad() {
		Assert.assertEquals(1, metric.score(new Chord(Note.C, Chord.MAJ7), new Chord(Note.C, Chord.MAJ)), E);
		Assert.assertEquals(1, metric.score(new Chord(Note.C, Chord.MAJ7), new Chord(Note.E, Chord.MIN)), E);
		Assert.assertEquals(0, metric.score(new Chord(Note.C, Chord.MAJ7), new Chord(Note.C, Chord.MIN)), E);
	}

	@Test
	public void testUnnamedChord() {
		Assert.assertEquals(1, metric.score(new Chord(Note.C, Note.D, Note.E, Note.F, Note.G), Chord.major(Note.C)), E);
		Assert.assertEquals(0, metric.score(new Chord(Note.C, Note.D, Note.E, Note.F, Note.G), Chord.minor(Note.C)), E);
	}

}
