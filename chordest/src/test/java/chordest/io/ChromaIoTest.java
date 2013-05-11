package chordest.io;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import chordest.io.chroma.ChromaFileReader;
import chordest.io.chroma.ChromaFileWriter;

public class ChromaIoTest {

	private static final String EXPECTED_FILENAME = "src/test/resources/05_-_Little_Child.wav.nnls";

	@Test
	public void testChromaRead() {
		ChromaFileReader br = new ChromaFileReader(new File(EXPECTED_FILENAME));
		Assert.assertEquals(2325, br.getTimestamps().length);
		Assert.assertEquals(2325, br.getChroma().length);
		Assert.assertTrue(Math.abs(br.getTimestamps()[1000] - 46.625668934) < 0.00001);
		Assert.assertTrue(Math.abs(br.getChroma()[1000][5] - 0.0288962) < 0.00001);
	}

	@Test
	public void testChromaWriteRead() throws IOException {
		double[] array = new double[] { 0.0, 0.6, 1.2 };
		double[][] chroma = new double[3][12];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 12; j++) {
				chroma[i][j] = 1.0 / (i + j + 1);
			}
		}
		File temp = File.createTempFile(this.getClass().getName(), ".chroma");
		ChromaFileWriter.write(temp.getAbsolutePath(), chroma, array);
		ChromaFileReader br = new ChromaFileReader(temp);
		double[] array2 = br.getTimestamps();
		double[][] chroma2 = br.getChroma();
		Assert.assertEquals(array.length, array2.length);
		for (int i = 0; i < array.length; i++) {
			Assert.assertTrue(Math.abs(array[i] - array2[i]) < 0.00001);
		}
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 12; j++) {
				Assert.assertTrue(Math.abs(chroma[i][j] - chroma2[i][j]) < 0.00001);
			}
		}
	}

}
