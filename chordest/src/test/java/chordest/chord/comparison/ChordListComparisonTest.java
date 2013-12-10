package chordest.chord.comparison;

import junit.framework.Assert;

import org.junit.Test;

import chordest.chord.comparison.ChordListsComparison;
import chordest.model.Chord;
import chordest.model.Note;


public class ChordListComparisonTest {

	@Test
	public void oudreExample() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.major(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 0.6, 1.2, 2.0};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.major(Note.F), Chord.major(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.7) < 0.00001);
		// ea = (1 - 0.6) + (1 - 0.8) = 0.6;  ae = (0.6 - 0.6) + (0.6 - 0.4) + (0.8 - 0.8) = 0.2
		// segm = 1 - max{0.6, 0.2}/2 = 1 - 0.3 = 0.7
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 0.7) < 0.00001);
	}

	@Test
	public void majorAndMinorWithSameRootShouldNotBeEqual() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 1.1, 2.0};
		Chord[] actualChords = new Chord[] { Chord.minor(Note.C), Chord.major(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(sim.getOverlapMeasure() < 0.00001);
		// ea = (1 - 1) + (1 - 0.9) = 0.1;  ae = (1.1 - 1) + (0.9 - 0.9) = 0.1
		// segm = 1 - max{0.1, 0.1}/2 = 1 - 0.05 = 0.95
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 0.95) < 0.00001);
	}

	@Test
	public void testStartFromNonZero() {
		double[] expectedTimestamps = new double[] {0.1, 1.1, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.1, 1.1, 2.0};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 1.0) < 0.00001);
		// ea = (1 - 1) + (0.9 - 0.9) = 0;  ae = (1 - 1) + (0.9 - 0.9) = 0
		// segm = 1 - max{0, 0}/2 = 1
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 1.0) < 0.00001);
	}

	@Test
	public void testStartFromDifferentNonZero() {
		double[] expectedTimestamps = new double[] {0.3, 1.2, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.4, 1.1, 1.9};
		Chord[] actualChords = new Chord[] { Chord.minor(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.5) < 0.00001);
		// ea = (0.9 - 0.7) + (0.8 - 0.7) = 0.3;  ae = (0.7 - 0.7) + (0.8 - 0.7) = 0.1
		// segm = 1 - max{0.3, 0.1}/2 = 1 - 0.15 = 0.85
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 0.85) < 0.00001);
	}

	@Test
	public void testHalfOverlap() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.5, 1.5, 1.5};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.5) < 0.00001);
		// ea = (1 - 0.5) + (1 - 0.5) = 1;  ae = (1 - 0.5) + (0 - 0) = 0.5
		// segm = 1 - max{1, 0.5}/2 = 1 - 0.5 = 0.5
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 0.5) < 0.00001);
	}

//	@Test
	public void test7th() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { new Chord(Note.C, Chord.MAJ7), new Chord(Note.A, Chord.MIN7) };
		double[] actualTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 1.0) < 0.00001);
		// ea = (1 - 1) + (1 - 1) = 0;  ae = (1 - 1) + (1 - 1) = 0
		// segm = 1 - max{0, 0}/2 = 1
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 1.0) < 0.00001);
	}

	@Test
	public void testUnknownChord() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), new Chord(Note.A, Chord.MAJ7) };
		double[] actualTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { new Chord(Note.C, Chord.MAJ), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.5) < 0.00001);
		// ea = (1 - 1) + (1 - 1) = 0;  ae = (1 - 1) + (1 - 1) = 0
		// segm = 1 - max{0, 0}/2 = 1
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 1.0) < 0.00001);
	}

	@Test
	public void testNoChordAtTheBeginning() {
		double[] expectedTimestamps = new double[] {0.1, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.major(Note.A) };
		double[] actualTimestamps = new double[] {0.2, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { Chord.minor(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.05) < 0.00001);
		// ea = (0.9 - 0.8) + (1 - 1) = 0.1;  ae = (0.8 - 0.8) + (1 - 1) = 0
		// segm = 1 - max{0.1, 0}/2 = 1 - 0.05 = 0.95
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 0.95) < 0.00001);
	}

	@Test
	public void testNoChordAtTheEnd() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 1.5};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps, new Mirex2010());
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.75) < 0.00001);
		// ea = (1 - 1) + (0.5 - 0.5) = 0;  ae = (1 - 1) + (1 - 0.5) = 0.5
		// segm = 1 - max{0, 0.5}/2 = 1 - 0.25 = 0.75
		Assert.assertTrue(Math.abs(sim.getSegmentation() - 0.75) < 0.00001);
	}

}
