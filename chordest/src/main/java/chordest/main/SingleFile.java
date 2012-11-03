package chordest.main;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordExtractor;
import chordest.chord.ChordListsComparison;
import chordest.configuration.Configuration;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.spectrum.FileSpectrumDataProvider;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.util.Visualizer;



public class SingleFile {

//	private static final String SEP = PathConstants.SEP;
//	private static final String ARTIST = "Beatles";
//	private static final String ALBUM = "05_-_Help!";
//	private static final String TRACK = "08_-_Act_Naturally";
//	private static final String TRACK_PATH = ARTIST + SEP + ALBUM + SEP + TRACK + PathConstants.EXT_WAV;
	private static final Logger LOG = LoggerFactory.getLogger(SingleFile.class);

	public static void main(String[] args) {
		if (args.length != 4) {
			LOG.error("Usage: SingleFile /path/to/testFile.wav /path/to/testFileSpectrum.bin /path/to/testFile.lab /path/to/results/");
			System.exit(-1);
		}
		String WAV_FILENAME = args[0];
		String SPECTRUM_FILENAME = args[1];
		String LAB_FILENAME = args[2];
		String RESULT_PATH = args[3];
		File wavFile = new File(WAV_FILENAME);
		String RESULT_FILENAME = RESULT_PATH + wavFile.getName() + ".txt";
		
		Configuration c = new Configuration();
		File spectrumFile = new File(SPECTRUM_FILENAME);
		ChordExtractor ce;
		if (spectrumFile.exists()) {
			ce = new ChordExtractor(c, new FileSpectrumDataProvider(SPECTRUM_FILENAME));
		} else {
			ce = new ChordExtractor(c, new WaveFileSpectrumDataProvider(WAV_FILENAME, c));
		}

		File labFile = new File(LAB_FILENAME);
		if (labFile.exists()) {
			LabFileReader labReader = new LabFileReader(labFile);
			ChordListsComparison sim = new ChordListsComparison(labReader.getChords(),
					labReader.getTimestamps(), ce.getChords(), ce.getOriginalBeatTimes());
			LOG.info("Overlap measure: " + sim.getOverlapMeasure());
		}
		
		int startOffset = ce.getStartNoteOffsetInSemitonesFromF0();
		Visualizer.visualizeChords(ce.getChords(), ce.getOriginalBeatTimes(), WAV_FILENAME, startOffset);
		
		LabFileWriter labWriter = new LabFileWriter(ce);
		try {
			labWriter.writeTo(new File(RESULT_FILENAME));
		} catch (IOException e) {
			LOG.error("Error when writing lab file", e);
		}
		
//		CsvFileWriter csvWriter = new CsvFileWriter(ce.getChords(), beatTimes);
//		try {
//			csvWriter.writeTo(new File(PathConstants.OUTPUT_DIR + csvFileName));
//		} catch (IOException e) {
//			LOG.error("Error when writing csv file", e);
//		}
	}

}
