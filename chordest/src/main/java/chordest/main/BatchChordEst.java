package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordExtractor;
import chordest.configuration.Configuration;
import chordest.configuration.LogConfiguration;
import chordest.io.beat.BeatFileWriter;
import chordest.io.lab.LabFileWriter;
import chordest.io.spectrum.SpectrumFileWriter;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.util.TracklistCreator;

public class BatchChordEst {

	private static final Logger LOG = LoggerFactory.getLogger(BatchChordEst.class);

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java -jar chordest.jar /path/to/testFileList.txt /path/to/scratch/dir /path/to/results/dir");
			System.exit(-1);
		}
		args[1] = addTrailingSeparatorIfMissing(args[1]);
		args[2] = addTrailingSeparatorIfMissing(args[2]);
		boolean saveSpectra = args.length > 3 && args[3] != null && args[3].contains("s");
		boolean saveBeats = args.length > 3 && args[3] != null && args[3].contains("b");
		
		LogConfiguration.setLogFileDirectory(args[1]);
		List<String> tracklist = TracklistCreator.readTrackList(args[0]);
		
		Configuration c = new Configuration();
		for (final String wavFileName : tracklist) {
			String labFileName = new File(wavFileName).getName() + ".txt";
			ChordExtractor ce = new ChordExtractor(c.process, new WaveFileSpectrumDataProvider(wavFileName, c.spectrum));

			LabFileWriter labWriter = new LabFileWriter(ce);
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
