package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.FileBeatBarTimesProvider;
import chordest.chord.ChordRecognizer;
import chordest.chord.ChromaExtractor;
import chordest.chord.comparison.Triads;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.configuration.LogConfiguration;
import chordest.io.lab.LabFileWriter;
import chordest.model.Chord;
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
			ChromaExtractor ce;
			if (new File(beatFileName).exists()) {
				ce = new ChromaExtractor(c.process, c.template, new WaveFileSpectrumDataProvider(
						wavFileName, beatFileName, c, new FileBeatBarTimesProvider(beatFileName)));
			} else {
				ce = new ChromaExtractor(c.process, c.template, new WaveFileSpectrumDataProvider(
						wavFileName, beatFileName, c));
			}
			ITemplateProducer producer = new TemplateProducer(ce.getStartNote(), c.template);
			ChordRecognizer cr = new ChordRecognizer(ce.getChroma(), ce.getNoChordness(), producer, c.process.noChordnessLimit, ce.getKey());
			Chord[] chords = cr.recognize(new Triads().getOutputTypes());
			
			LabFileWriter labWriter = new LabFileWriter(chords, ce.getOriginalBeatTimes());
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
