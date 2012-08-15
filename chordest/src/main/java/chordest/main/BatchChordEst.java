package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.BeatFileWriter;
import chordest.beat.BeatRootAdapter;
import chordest.chord.ChordExtractor;
import chordest.lab.LabFileWriter;
import chordest.properties.Configuration;
import chordest.spectrum.SpectrumFileWriter;
import chordest.util.TracklistCreator;

public class BatchChordEst {

	private static final Logger LOG = LoggerFactory.getLogger(BatchChordEst.class);

	public static void main(String[] args) {
		if (args.length < 3) {
			LOG.error("Usage: BatchChordEst /path/to/testFileList.txt /path/to/scratch/dir /path/to/results/dir");
			LOG.error("Second argument will be ignored for now, but must be present");
			System.exit(-1);
		}
		args[1] = addTrailingSeparatorIfMissing(args[1]);
		args[2] = addTrailingSeparatorIfMissing(args[2]);
		boolean saveSpectra = args.length > 3 && args[3] != null && args[3].contains("s");
		boolean saveBeats = args.length > 3 && args[3] != null && args[3].contains("b");
		
		List<String> tracklist = TracklistCreator.readTrackList(args[0]);
		
		Configuration c = new Configuration("parameters.properties");
		for (final String wavFileName : tracklist) {
			String labFileName = new File(wavFileName).getName() + ".txt";
			BeatRootAdapter beatRoot = new BeatRootAdapter(wavFileName, null);
			ChordExtractor ce = new ChordExtractor(c, wavFileName, beatRoot);

			LabFileWriter labWriter = new LabFileWriter(ce.getChords(), ce.getOriginalBeatTimes());
			try {
				labWriter.writeTo(new File(args[2] + labFileName));
			} catch (IOException e) {
				LOG.error("Error when saving lab file", e);
			}
			
			if (saveSpectra) {
				String spectrumFilePath = args[1] + new File(wavFileName).getName() + ".bin";
				SpectrumFileWriter.write(spectrumFilePath, ce.getSpectrum());
			}
			if (saveBeats) {
				String fileName = args[1] + new File(wavFileName).getName() + ".beat";
				BeatFileWriter.write(fileName, ce.getOriginalBeatTimes());
			}
			
			LOG.info("Done: " + labFileName);
		}
		LOG.info(tracklist.size() + " files have been processed. The end.");
	}

	public BatchChordEst() {
		
	}

	private static String addTrailingSeparatorIfMissing(String str) {
		if (! str.endsWith(File.separator)) {
			return str + File.separator;
		}
		return str;
	}

}
