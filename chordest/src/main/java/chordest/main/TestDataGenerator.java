package chordest.main;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class TestDataGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(TestDataGenerator.class);

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: TestDataGenerator /path/to/testFileList.txt /output/folder");
			System.exit(-1);
		}
		
		List<String> tracklist = TracklistCreator.readTrackList(args[0]);
		String outputFolder = args[1].trim();
		if (! outputFolder.endsWith(File.separator)) {
			outputFolder = outputFolder + File.separator;
		}
		
		int filesProcessed = 0;
		for (final String binFileName : tracklist) {
			String csvFileName = outputFolder + new File(binFileName).getName() + PathConstants.EXT_CSV;
			TrainDataGenerator.deleteIfExists(csvFileName);
			TrainDataGenerator tdg = new TrainDataGenerator(csvFileName, false);
			double[][] result = TrainDataGenerator.prepareSpectrum(binFileName);
			tdg.process(result);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed. Resulting files were saved to " + outputFolder);
	}

}
