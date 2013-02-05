package chordest.chord.recognition;

import junit.framework.Assert;

import org.junit.Test;

import chordest.io.lab.chordparser.ChordParser;
import chordest.io.lab.chordparser.ParseException;
import chordest.io.lab.chordparser.TokenMgrError;
import chordest.model.Chord;
import chordest.model.Note;


public class TemplatesRecognitionTest {

	@Test
	public void isKnownWithoutMajorWrongNotesOrder() {
		Assert.assertTrue(TemplatesRecognition.isKnown(new Chord(Note.E, Note.G, Note.C)));
	}

	@Test
	public void isKnownEmptyChord() {
		Assert.assertTrue(TemplatesRecognition.isKnown(Chord.empty()));
		Assert.assertTrue(TemplatesRecognition.isKnown(new Chord(Note.F, Chord.N)));
	}

	@Test
	public void isKnownMajorMinor() {
		Assert.assertTrue(TemplatesRecognition.isKnown(Chord.major(Note.E)));
		Assert.assertTrue(TemplatesRecognition.isKnown(new Chord(Note.F, Chord.MAJ)));
		Assert.assertTrue(TemplatesRecognition.isKnown(Chord.minor(Note.FD)));
		Assert.assertTrue(TemplatesRecognition.isKnown(new Chord(Note.B, Chord.MIN)));
	}

	@Test
	public void isKnownMajorWithoutShorthand() throws NumberFormatException, ParseException, TokenMgrError {
		Assert.assertTrue(TemplatesRecognition.isKnown(ChordParser.parseString("D#")));
	}

	@Test
	public void isKnownMajorWithShorthand() throws NumberFormatException, ParseException, TokenMgrError {
		Assert.assertTrue(TemplatesRecognition.isKnown(ChordParser.parseString("D#:maj")));
	}

	@Test
	public void isKnownAugmentedDiminished() {
		Assert.assertTrue(TemplatesRecognition.isKnown(new Chord(Note.B, Chord.AUG)));
		Assert.assertTrue(TemplatesRecognition.isKnown(new Chord(Note.AD, Chord.DIM)));
	}

	@Test
	public void otherChordsAreNotKnown() {
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.DIM7)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.DOM)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.HDIM7)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.MAJ6)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.MAJ7)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.MAJ9)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.MIN6)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.MIN7)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.MIN9)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.MINMAJ7)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.NON)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.SUS2)));
		Assert.assertFalse(TemplatesRecognition.isKnown(new Chord(Note.A, Chord.SUS4)));
	}

}
