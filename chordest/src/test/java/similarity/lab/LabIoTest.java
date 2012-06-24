package similarity.lab;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import similarity.chord.Chord;
import utils.PathConstants;

public class LabIoTest {

	private static final String SEP = PathConstants.SEP;
	private static final String ARTIST = "Beatles";
	private static final String ALBUM = "01_-_Please_Please_Me";
	private static final String TRACK = "05_-_Boys";
	private static final String LAB_EXPECTED_FILENAME = PathConstants.LAB_DIR
			+ ARTIST + SEP + ALBUM + SEP + TRACK + PathConstants.EXT_LAB;

	@Test
	public void testLabReadWrite() {
		LabFileReader reader = new LabFileReader(new File(LAB_EXPECTED_FILENAME));
		Chord[] expectedChords = reader.getChords();
		double[] expectedTimestamps = reader.getTimestamps();
		LabFileWriter writer = new LabFileWriter(expectedChords, expectedTimestamps);
		try {
			File temp = File.createTempFile("chords", ".tmp");
			writer.writeTo(temp);
			LabFileReader reader2 = new LabFileReader(temp);
			Assert.assertTrue(Arrays.equals(expectedChords, reader2.getChords()));
			Assert.assertTrue(Arrays.equals(expectedTimestamps, reader2.getTimestamps()));
		} catch (IOException e) {
			Assert.fail();
		}
	}
}
