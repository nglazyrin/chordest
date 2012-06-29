package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.BeatRootAdapter;
import chordest.chord.ChordExtractor;
import chordest.lab.LabFileWriter;
import chordest.properties.Configuration;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class BatchChordExtractor {

	private static final Logger LOG = LoggerFactory.getLogger(Roundtrip.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Configuration c = new Configuration("config" + PathConstants.SEP + "parameters.properties");
		List<String> tracklist = TracklistCreator.createTracklist(new File(c.directory.wav), "", PathConstants.EXT_WAV);
		for (final String wavFileName : tracklist) {
			final String labFileName = "results" + PathConstants.SEP + 
					wavFileName.replace(PathConstants.EXT_WAV, PathConstants.EXT_LAB);
			String fullWavFileName = c.directory.wav + wavFileName;
			BeatRootAdapter beatRoot = new BeatRootAdapter(fullWavFileName, null);
			ChordExtractor ce = new ChordExtractor(c, fullWavFileName, beatRoot);

			LabFileWriter labWriter = new LabFileWriter(ce.getChords(), ce.getOriginalBeatTimes());
			try {
				labWriter.writeTo(new File(PathConstants.OUTPUT_DIR + labFileName));
			} catch (IOException e) {
				LOG.error("Error when saving lab file", e);
			}
			LOG.info("\u2713 " + labFileName);
		}
		LOG.info("Directory has been processed: " + c.directory.wav);
	}

}
