package chordest.io;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import chordest.io.csv.CsvSpectrumFileReader;
import chordest.io.csv.CsvSpectrumFileWriter;

public class CsvSpectrumIoTest {

	@Test
	public void testInputOutput() throws IOException {
		File expected = new File("src/test/resources/spectrum.csv");
		CsvSpectrumFileReader reader = new CsvSpectrumFileReader(expected);
		double[][] spectrum = reader.getSpectrum();
		File actual = File.createTempFile("csvSpectrumIoTest-", ".csv");
		CsvSpectrumFileWriter writer = new CsvSpectrumFileWriter(spectrum);
		writer.writeTo(actual);
		
		compareFiles(expected, actual);
	}

	private void compareFiles(File expectedFile, File actualFile) throws IOException {
		String expected = FileUtils.readFileToString(expectedFile);
		String actual = FileUtils.readFileToString(actualFile);
		Assert.assertEquals(expected, actual);
	}

	

}
