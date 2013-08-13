package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.spectrum.SpectrumFileReader;
import chordest.spectrum.SpectrumData;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

/**
 * Stores spectrum values in csv file, 1 csv file per track.
 * @author Nikolay
 *
 */
public class TestDataGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(TestDataGenerator.class);
	public static final String TEST_FILE_LIST = PathConstants.RESOURCES_DIR + "filelists" + PathConstants.SEP + "bqz_bin0test.txt";
	private static final String OUTPUT_FOLDER = PathConstants.CSV_DIR + "test" + PathConstants.SEP;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList(TEST_FILE_LIST);
		
		int filesProcessed = 0;
		try {
			FileUtils.cleanDirectory(new File(OUTPUT_FOLDER));
		} catch (IOException e) {
			LOG.error("Could not clean " + OUTPUT_FOLDER, e);
		}
		for (final String binFileName : tracklist) {
			String csvFileName = OUTPUT_FOLDER + new File(binFileName).getName() + PathConstants.EXT_CSV;
			TrainDataGenerator.deleteIfExists(csvFileName);
			TrainDataGenerator tdg = new TrainDataGenerator(csvFileName, false);
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] result = TrainDataGenerator.prepareSpectrum(sd);
			tdg.process(result, 0, TrainDataGenerator.INPUTS + 12);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed. Resulting files were saved to " + OUTPUT_FOLDER);
	}

}
