package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordExtractor;
import chordest.chord.ChordExtractor.IExternalProcessor;
import chordest.chord.ChordListsComparison;
import chordest.chord.ComparisonAccumulator;
import chordest.configuration.Configuration;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.spectrum.CsvFileSpectrumDataProvider;
import chordest.spectrum.FileSpectrumDataProvider;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class TestDenoisingAutoencoder {

	private static final Logger LOG = LoggerFactory.getLogger(TestDenoisingAutoencoder.class);

	private static String BIN_DIRECTORY = "spectrum8" + PathConstants.SEP;
	private static String CSV_DIRECTORY = PathConstants.CSV_DIR + "encoded" + PathConstants.SEP;

	private static ComparisonAccumulator denoised_acc = new ComparisonAccumulator();
	private static ComparisonAccumulator acc = new ComparisonAccumulator();
	private static Configuration c = new Configuration();

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList("work" + PathConstants.SEP + "all_files1.txt");
//		List<String> tracklist = TracklistCreator.createTracklist(new File(BIN_DIRECTORY), "", PathConstants.EXT_BIN);
		for (String item : tracklist) {
			String track = StringUtils.substringAfterLast(item, PathConstants.SEP);
			String binFile = BIN_DIRECTORY + track;
			String csvFile = CSV_DIRECTORY + track + PathConstants.EXT_CSV;
			String labFile = PathConstants.LAB_DIR + StringUtils.replace(track, PathConstants.EXT_WAV + PathConstants.EXT_BIN, PathConstants.EXT_LAB);
			processWithDenoising(binFile, csvFile, labFile);
//			processWithoutDenoising(binFile, labFile);
		}
		
//		LOG.info("Original:");
//		LOG.info("AOR: " + acc.getAOR());
//		LOG.info("WAOR: " + acc.getWAOR());
		
		LOG.info("Denoised:");
		LOG.info("AOR: " + denoised_acc.getAOR());
		LOG.info("WAOR: " + denoised_acc.getWAOR());
	}

	private static void processWithDenoising(final String binFile, final String csvFile, String expectedLab) {
		ChordExtractor ce = new ChordExtractor(c.process, new FileSpectrumDataProvider(binFile), new IExternalProcessor() {
			@Override
			public double[][] process(double[][] data) {
				return new CsvFileSpectrumDataProvider(binFile, csvFile).getSpectrumData().spectrum;
			}
		});
		double rco = process(ce, denoised_acc, new File(expectedLab));
		LOG.info(binFile + " DRCO: " + rco);
	}

	private static void processWithoutDenoising(String binFile, String expectedLab) {
		ChordExtractor ce = new ChordExtractor(c.process, new FileSpectrumDataProvider(binFile));
		double rco = process(ce, acc, new File(expectedLab));
		LOG.info(binFile + " RCO: " + rco);
	}

	private static double process(ChordExtractor ce, ComparisonAccumulator accumulator, File expectedLab) {
		LabFileWriter labWriter = new LabFileWriter(ce);
		File temp = null;
		try {
			temp = File.createTempFile("chordest", PathConstants.EXT_LAB);
			labWriter.writeTo(temp);
		} catch (IOException e) {
			LOG.error("Error when saving temporary lab file", e);
			System.exit(-1);
		}

		LabFileReader labReaderExpected = new LabFileReader(expectedLab);
		LabFileReader labReaderActual = new LabFileReader(temp);
		ChordListsComparison cmp = new ChordListsComparison(labReaderExpected.getChords(),
				labReaderExpected.getTimestamps(), labReaderActual.getChords(), labReaderActual.getTimestamps());
		accumulator.append(cmp);
		
		return cmp.getOverlapMeasure();
	}

}
