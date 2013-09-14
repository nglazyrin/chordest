package chordest.chord.comparison;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import chordest.io.lab.chordparser.ChordParser;
import chordest.io.lab.chordparser.ParseException;
import chordest.io.lab.chordparser.TokenMgrError;
import chordest.model.Chord;
import chordest.model.Note;

public class TetradsTest {

	private static double E = 1e-5;

	private Tetrads metric;

	@Before
	public void setUp() {
		metric = new Tetrads();
	}

	@Test
	public void testNoChords() {
		Assert.assertEquals(Chord.empty(), metric.map(Chord.empty()));
		Assert.assertEquals(1, metric.score(Chord.empty(), Chord.empty()), E);
	}

	@Test
	public void testAugDimSus() throws NumberFormatException, ParseException, TokenMgrError {
		Assert.assertEquals(null, metric.map(new Chord(Note.C, Chord.AUG)));
		Assert.assertEquals(null, metric.map(new Chord(Note.C, Chord.DIM)));
		Assert.assertEquals(null, metric.map(new Chord(Note.C, Chord.SUS4)));
		Assert.assertEquals(null, metric.map(new Chord(Note.C, Chord.SUS2)));
		Assert.assertEquals(null, metric.map(ChordParser.parseString("F:sus4(9)")));
	}

	@Test
	public void testMapToMajMin() throws NumberFormatException, ParseException, TokenMgrError {
		Chord cMaj = Chord.major(Note.C);
		Chord cMin = Chord.minor(Note.C);
		Assert.assertEquals(cMaj, metric.map(new Chord(Note.C, Note.E, Note.G)));
		Assert.assertEquals(cMaj, metric.map(new Chord(Note.C, Note.E, Note.G, Note.GD)));
		Assert.assertEquals(cMaj, metric.map(new Chord(Note.C, Note.E, Note.G, Note.C)));
		Assert.assertEquals(cMaj, metric.map(new Chord(Note.C, Note.E, Note.G, Note.F)));
		Assert.assertEquals(1, metric.score(cMaj, new Chord(Note.C, Note.G, Note.E)), E);
		
		Assert.assertEquals(cMin, metric.map(new Chord(Note.C, Note.DD, Note.G)));
		Assert.assertEquals(cMin, metric.map(new Chord(Note.C, Note.DD, Note.G, Note.A)));
		Assert.assertEquals(cMin, metric.map(new Chord(Note.C, Note.DD, Note.G, Note.B)));
		Assert.assertEquals(cMin, metric.map(new Chord(Note.C, Note.DD, Note.G, Note.F)));
		Assert.assertEquals(1, metric.score(cMin, new Chord(Note.DD, Note.G, Note.C)), E);
		
//		Assert.assertEquals(Chord.major(Note.G), metric.map(ChordParser.parseString("G:7(#9)")));
	}

	@Test
	public void testMapTo7th() throws NumberFormatException, ParseException, TokenMgrError {
		Chord cMaj7 = new Chord(Note.C, Chord.MAJ7);
		Chord cMin7 = new Chord(Note.C, Chord.MIN7);
		Chord c7 = new Chord(Note.C, Chord.DOM);
		
		Assert.assertEquals(cMaj7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.B)));
		Assert.assertEquals(cMaj7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.B, Note.D)));
		Assert.assertEquals(cMaj7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.B, Note.C)));
		Assert.assertEquals(cMaj7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.B, Note.F)));
		Assert.assertEquals(1, metric.score(cMaj7, new Chord(Note.B, Note.G, Note.E, Note.C)), E);
		
		Assert.assertEquals(cMin7, metric.map(new Chord(Note.C, Note.DD, Note.G, Note.AD)));
		Assert.assertEquals(cMin7, metric.map(new Chord(Note.C, Note.DD, Note.G, Note.AD, Note.D)));
		Assert.assertEquals(cMin7, metric.map(new Chord(Note.C, Note.DD, Note.G, Note.AD, Note.B)));
		Assert.assertEquals(cMin7, metric.map(new Chord(Note.C, Note.DD, Note.G, Note.AD, Note.F)));
		Assert.assertEquals(1, metric.score(cMin7, new Chord(Note.G, Note.DD, Note.C, Note.AD)), E);
		
		Assert.assertEquals(c7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.AD)));
		Assert.assertEquals(c7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.AD, Note.D)));
		Assert.assertEquals(c7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.AD, Note.B)));
		Assert.assertEquals(c7, metric.map(new Chord(Note.C, Note.E, Note.G, Note.AD, Note.F)));
		Assert.assertEquals(1, metric.score(c7, new Chord(Note.G, Note.C, Note.AD, Note.E)), E);
		
		Assert.assertEquals(new Chord(Note.G, Chord.DOM), metric.map(ChordParser.parseString("G:7(#9)")));
	}

}
