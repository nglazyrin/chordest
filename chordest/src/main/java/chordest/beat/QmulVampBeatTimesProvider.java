package chordest.beat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class QmulVampBeatTimesProvider implements IBeatTimesProvider {

	private static Logger LOG = LoggerFactory.getLogger(QmulVampBeatTimesProvider.class);

	private static final String TARGET_DIR = PathConstants.BEAT_DIR;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList("work\\all_wav.txt");
		int filesProcessed = 0;
		for (String wavFilePath : tracklist) {
			new QmulVampBeatTimesProvider(wavFilePath);
	        if (++filesProcessed % 10 == 0) {
	        	LOG.info(filesProcessed + " files were processed");
	        }
		}
		LOG.info("Done. " + filesProcessed + " files were saved to '" + TARGET_DIR + "'");
	}

	private double[] beats = new double[0];

	public QmulVampBeatTimesProvider(String wavFilePath) {
		LOG.info("Performing beat detection for " + wavFilePath + " ...");
		String track = StringUtils.substringAfterLast(wavFilePath, File.separator);
		String beatFilePath = TARGET_DIR + track + PathConstants.EXT_BEAT;
		String[] cmd = { "work\\vamp\\vamp-simple-host.exe", "qm-vamp-plugins:qm-barbeattracker:beats", wavFilePath, "-o", beatFilePath };
		try {
			Process p = Runtime.getRuntime().exec(cmd);
	        p.waitFor();
	        LOG.error(IOUtils.toString(p.getErrorStream()));
	        beats = new FileBeatBarTimesProvider(beatFilePath).getBeatTimes();
		} catch (IOException e) {
			LOG.error("Error when reading wave file to detect beats", e);
		} catch (InterruptedException e) {
			LOG.error("Interrupted when reading wave file to detect beats", e);
		}
	}

	@Override
	public double[] getBeatTimes() {
		return beats;
	}

}
