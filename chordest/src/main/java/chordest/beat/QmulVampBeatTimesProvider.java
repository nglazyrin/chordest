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

/**
 * A class that runs QMUL (Davies) beat tracker to obtain beat sequence for a
 * given wave file. Vamp simple host is required and qm-vamp-plugins need to be
 * installed on your system. See the string constants below.
 * @author Nikolay
 *
 */
public class QmulVampBeatTimesProvider implements IBeatTimesProvider {

	private static Logger LOG = LoggerFactory.getLogger(QmulVampBeatTimesProvider.class);

	private static final String TARGET_DIR = PathConstants.BEAT_DIR;

	private static final String PATH_TO_EXE = "work\\vamp\\vamp-simple-host.exe";

	private static final String PLUGIN_NAME = "qm-vamp-plugins:qm-barbeattracker:beats";

	/**
	 * Quick and dirty way to do beat extraction in batch mode.
	 * @param args
	 */
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
		String[] cmd = { PATH_TO_EXE, PLUGIN_NAME, wavFilePath, "-o", beatFilePath };
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
