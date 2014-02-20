package chordest.beat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.configuration.Configuration;
import chordest.configuration.Configuration.PreProcessProperties;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

/**
 * A class that runs QMUL (Davies) beat tracker to obtain beat sequence for a
 * given wave file. Vamp simple host is required and qm-vamp-plugins need to be
 * installed on your system. See the string constants below.
 * @author Nikolay
 *
 */
public class VampBeatTimesProvider implements IBeatTimesProvider {

	private static Logger LOG = LoggerFactory.getLogger(VampBeatTimesProvider.class);

	private static final String TARGET_DIR = PathConstants.BEAT_DIR;

	private static final String BEATROOT_PLUGIN_NAME = "beatroot-vamp:beatroot";

	private static final String QMBARBEAT_PLUGIN_NAME = "qm-vamp-plugins:qm-barbeattracker:beats";

	private static final String MVAMPIBT_PLUGIN_NAME = "mvamp-ibt:marsyas_ibt";

	private static final String[] PLUGINS = new String[] {
		BEATROOT_PLUGIN_NAME,
		QMBARBEAT_PLUGIN_NAME,
		MVAMPIBT_PLUGIN_NAME
	};

	/**
	 * Quick and dirty way to do beat extraction in batch mode.
	 * @param args
	 */
	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList("work\\all_wav.txt");
		int filesProcessed = 0;
		for (String wavFilePath : tracklist) {
			try {
				String track = StringUtils.substringAfterLast(wavFilePath, File.separator);
				String beatFilePath = TARGET_DIR + track + PathConstants.EXT_BEAT;
				new VampBeatTimesProvider(wavFilePath, beatFilePath, new Configuration().pre);
			} catch (Exception e) {
				e.printStackTrace();
			}
	        if (++filesProcessed % 10 == 0) {
	        	LOG.info(filesProcessed + " files were processed");
	        }
		}
		LOG.info("Done. " + filesProcessed + " files were saved to '" + TARGET_DIR + "'");
	}

	private double[] beats = new double[0];

	public VampBeatTimesProvider(String wavFilePath, String beatFilePath, PreProcessProperties pre) throws IOException, InterruptedException {
		LOG.info("Performing beat detection for " + wavFilePath + " ...");
		for (int i = 0; i < PLUGINS.length; i++) {
			String[] cmd = { pre.vampHostPath, PLUGINS[i], wavFilePath, "-o", beatFilePath };
			Process p = Runtime.getRuntime().exec(cmd);
	        p.waitFor();
	        LOG.error(IOUtils.toString(p.getErrorStream()));
	        beats = new FileBeatBarTimesProvider(beatFilePath).getBeatTimes();
	        if (beats.length > 0) {
	        	break;
	        } else {
	        	LOG.warn(String.format("Plugin %s failed to detect beats for file '%s', trying %s",
	        			PLUGINS[i], wavFilePath, i < PLUGINS.length - 1 ? PLUGINS[i+1] : "to generate dummy beat sequence with step = 0.5 s"));
	        }
		}
	}

	@Override
	public double[] getBeatTimes() {
		return beats;
	}

}
