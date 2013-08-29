package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.FileBeatBarTimesProvider;
import chordest.chord.ChordExtractor;
import chordest.configuration.Configuration;
import chordest.configuration.LogConfiguration;
import chordest.io.lab.LabFileWriter;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

/**
 * This class is run during MIREX testing. It gets a list of files, performs
 * chord estimation, writes the results.
 * @author Nikolay
 *
 */
public class BatchChordEst {

	private static final Logger LOG = LoggerFactory.getLogger(BatchChordEst.class);

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java -jar chordest.jar /path/to/testFileList.txt /path/to/scratch/dir /path/to/results/dir");
			System.exit(-1);
		}
		args[1] = addTrailingSeparatorIfMissing(args[1]);
		args[2] = addTrailingSeparatorIfMissing(args[2]);
		
		LogConfiguration.setLogFileDirectory(args[1]);
		List<String> tracklist = TracklistCreator.readTrackList(args[0]);
		
		Configuration c = new Configuration();
		for (final String wavFileName : tracklist) {
			String trackName = new File(wavFileName).getName();
			String labFileName = trackName + ".txt";
			String beatFileName = args[1] + trackName + PathConstants.EXT_BEAT;
			ChordExtractor ce;
			if (new File(beatFileName).exists()) {
				ce = new ChordExtractor(c.process, new WaveFileSpectrumDataProvider(
						wavFileName, c, new FileBeatBarTimesProvider(beatFileName)));
			} else {
				ce = new ChordExtractor(c.process, new WaveFileSpectrumDataProvider(
						wavFileName, c));
			}
			
			LabFileWriter labWriter = new LabFileWriter(ce);
			try {
				labWriter.writeTo(new File(args[2] + labFileName));
			} catch (IOException e) {
				LOG.error("Error when saving lab file", e);
			}
			
			LOG.info("Done: " + labFileName);
		}
		LOG.info(tracklist.size() + " files have been processed. The end.");
	}

	private static String addTrailingSeparatorIfMissing(String str) {
		if (! str.endsWith(File.separator)) {
			return str + File.separator;
		}
		return str;
	}

}
