package chordest.io;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.model.Chord;
import chordest.model.Note;


public class LabIoTest {

	private static final String LAB_EXPECTED_FILENAME = "src/test/resources/05_-_Boys.lab";

	@Test
	public void testLabReadWrite() {
		LabFileReader reader = new LabFileReader(new File(LAB_EXPECTED_FILENAME));
		Chord[] expectedChords = reader.getChords();
		double[] expectedTimestamps = reader.getTimestamps();
		LabFileWriter writer = new LabFileWriter(expectedChords, expectedTimestamps);
		try {
			File temp = File.createTempFile("labIoTest", ".lab");
			writer.writeTo(temp);
			LabFileReader reader2 = new LabFileReader(temp);
			Assert.assertTrue(Arrays.equals(expectedChords, reader2.getChords()));
			Assert.assertTrue(Arrays.equals(expectedTimestamps, reader2.getTimestamps()));
		} catch (IOException e) {
			Assert.fail();
		}
	}
	
	@Test
	public void testGetChord() {
		LabFileReader reader = new LabFileReader(new File(LAB_EXPECTED_FILENAME));
		Assert.assertEquals(Chord.empty(), reader.getChord(-1, 0.5));
		Assert.assertEquals(Chord.empty(), reader.getChord(0.5, 0.5));
		Assert.assertEquals(Chord.empty(), reader.getChord(148, 0.5));
		
		Assert.assertEquals(Chord.major(Note.A), reader.getChord(22.1, 0.5));
		Assert.assertEquals(Chord.major(Note.E), reader.getChord(30, 0.5));
		Assert.assertEquals(null, reader.getChord(22.09, 0.5));
	}
}
