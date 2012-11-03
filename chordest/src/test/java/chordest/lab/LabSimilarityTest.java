package chordest.lab;

import junit.framework.Assert;

import org.junit.Test;

import chordest.chord.ChordListsComparison;
import chordest.model.Chord;
import chordest.model.Note;


public class LabSimilarityTest {

	@Test
	public void oudreExample() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.major(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 0.6, 1.2, 2.0};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.major(Note.F), Chord.major(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.7) < 0.00001);
	}

	@Test
	public void majorAndMinorWithSameRootShouldNotBeEqual() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 1.1, 2.0};
		Chord[] actualChords = new Chord[] { Chord.minor(Note.C), Chord.major(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(sim.getOverlapMeasure() < 0.00001);
	}

	@Test
	public void testStartFromNonZero() {
		double[] expectedTimestamps = new double[] {0.1, 1.1, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.1, 1.1, 2.0};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 1.0) < 0.00001);
	}

	@Test
	public void testStartFromDifferentNonZero() {
		double[] expectedTimestamps = new double[] {0.3, 1.2, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.4, 1.1, 1.9};
		Chord[] actualChords = new Chord[] { Chord.minor(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.5) < 0.00001);
	}

	@Test
	public void testHalfOverlap() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.5, 1.5, 1.5};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.5) < 0.00001);
	}

	@Test
	public void test7th() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { new Chord(Note.C, Chord.MAJ7), new Chord(Note.A, Chord.MIN7) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.0) < 0.00001);
	}

	@Test
	public void testUnknownChord() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { new Chord(Note.C, Chord.MAJ), new Chord(Note.A, Chord.MAJ7) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.5) < 0.00001);
	}

	@Test
	public void testNoChordAtTheBeginning() {
		double[] expectedTimestamps = new double[] {0.1, 1.0, 2.0};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.major(Note.A) };
		double[] actualTimestamps = new double[] {0.2, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { Chord.minor(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.05) < 0.00001);
	}

	@Test
	public void testNoChordAtTheEnd() {
		double[] expectedTimestamps = new double[] {0.0, 1.0, 1.5};
		Chord[] expectedChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		double[] actualTimestamps = new double[] {0.0, 1.0, 2.0};
		Chord[] actualChords = new Chord[] { Chord.major(Note.C), Chord.minor(Note.A) };
		
		ChordListsComparison sim = new ChordListsComparison(expectedChords, expectedTimestamps, 
				actualChords, actualTimestamps);
		Assert.assertTrue(Math.abs(sim.getOverlapMeasure() - 0.75) < 0.00001);
	}

}
