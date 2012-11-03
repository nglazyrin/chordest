package chordest.beat;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.beat.BeatFileReader;

public class FileBeatTimesProvider implements IBeatTimesProvider {

	private static Logger LOG = LoggerFactory.getLogger(FileBeatTimesProvider.class);

	private final double[] beatTimes;

	public FileBeatTimesProvider(String beatFilePath) {
		if (beatFilePath != null) {
			File beatFile = new File(beatFilePath);
			if (beatFile != null && beatFile.exists()) {
				LOG.debug("Reading beats from " + beatFilePath + "...");
				beatTimes = readBeats(beatFile);
			} else {
				beatTimes = new double[0];
			}
		} else {
			beatTimes = new double[0];
		}
	}

	@Override
	public double[] getBeatTimes() {
		return beatTimes;
	}

	private double[] readBeats(File file) {
		if (file == null) {
			throw new IllegalArgumentException("Cannot write to null file");
		}
		return new BeatFileReader(file).getTimestamps();
	}

}
