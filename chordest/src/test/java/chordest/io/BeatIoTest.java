package chordest.io;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import chordest.beat.FileBeatBarTimesProvider;
import chordest.io.beat.Beat2FileReader;
import chordest.io.beat.BeatFileWriter;

public class BeatIoTest {

	private static final String EXPECTED_FILENAME = "src/test/resources/09 Who Wants To Live Forever.wav.beat";

	@Test
	public void testBeatRead() {
		Beat2FileReader br = new Beat2FileReader(new File(EXPECTED_FILENAME));
		Assert.assertEquals(729, br.getTimestamps().length);
		Assert.assertTrue(Math.abs(br.getTimestamps()[100] - 43.073015873) < 0.00001);
	}

	@Test
	public void testFileBeatBarTimesProvider() {
		FileBeatBarTimesProvider p = new FileBeatBarTimesProvider(EXPECTED_FILENAME);
		Assert.assertEquals(729, p.getBeatTimes().length);
		Assert.assertTrue(Math.abs(p.getBeatTimes()[100] - 43.073015873) < 0.00001);
	}

	@Test
	public void testBeatWriteRead() throws IOException {
		double[] array = new double[] { 0.0, 0.6, 1.2, 2.4, 4.8, 10.0 };
		File temp = File.createTempFile(this.getClass().getName(), ".beat");
		BeatFileWriter.write(temp.getAbsolutePath(), array);
		Beat2FileReader br = new Beat2FileReader(temp);
		double[] array2 = br.getTimestamps();
		int[] bars = br.getBars();
		Assert.assertEquals(array.length, array2.length);
		for (int i = 0; i < array.length; i++) {
			Assert.assertTrue(Math.abs(array[i] - array2[i]) < 0.00001);
			Assert.assertEquals(i % 4 + 1, bars[i]);
		}
	}

}
