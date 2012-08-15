package chordest.main;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.BeatRootAdapter;
import chordest.chord.ChordExtractor;
import chordest.lab.LabFileReader;
import chordest.lab.LabFileWriter;
import chordest.lab.LabSimilarity;
import chordest.properties.Configuration;
import chordest.util.PathConstants;
import chordest.util.Visualizer;



public class SingleFile {

	private static final String SEP = PathConstants.SEP;
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
		
		Configuration c = new Configuration("config" + SEP + "parameters.properties");
		File spectrumFile = new File(SPECTRUM_FILENAME);
		ChordExtractor ce;
		if (spectrumFile.exists()) {
			ce = new ChordExtractor(c, SPECTRUM_FILENAME);
		} else {
			BeatRootAdapter beatRoot = new BeatRootAdapter(WAV_FILENAME, null);
			ce = new ChordExtractor(c, WAV_FILENAME, beatRoot);
		}

		File labFile = new File(LAB_FILENAME);
		if (labFile.exists()) {
			LabFileReader labReader = new LabFileReader(labFile);
			LabSimilarity sim = new LabSimilarity(labReader.getChords(),
					labReader.getTimestamps(), ce.getChords(), ce.getOriginalBeatTimes());
			LOG.info("Overlap measure: " + sim.getOverlapMeasure());
		}
		
		int startOffset = ce.getStartNoteOffsetInSemitonesFromF0();
		Visualizer.visualizeChords(ce.getChords(), ce.getOriginalBeatTimes(), WAV_FILENAME, startOffset);
		
		double[] beatTimes = ce.getOriginalBeatTimes();
		
		LabFileWriter labWriter = new LabFileWriter(ce.getChords(), beatTimes);
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
