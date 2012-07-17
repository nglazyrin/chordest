package chordest.main;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.BeatRootAdapter;
import chordest.chord.ChordExtractor;
import chordest.lab.CsvFileWriter;
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
	private static final String TRACK_PATH = "yellow.wav";
	
	private static final String LAB_FILENAME = PathConstants.LAB_DIR + 
			TRACK_PATH.replace(PathConstants.EXT_WAV, PathConstants.EXT_LAB);
	private static final String SPECTRUM_DIR = "spectrum_tuning" + SEP;
	private static final String SPECTRUM_FILENAME = SPECTRUM_DIR + 
			TRACK_PATH.replace(PathConstants.EXT_WAV, PathConstants.EXT_BIN);
	private static final Logger LOG = LoggerFactory.getLogger(SingleFile.class);

	public static void main(String[] args) {
		Configuration c = new Configuration("config" + SEP + "parameters.properties");
		String FILENAME = c.directory.wav + TRACK_PATH;
		File spectrumFile = new File(SPECTRUM_FILENAME);
		ChordExtractor ce;
		if (spectrumFile.exists()) {
			ce = new ChordExtractor(c, SPECTRUM_FILENAME);
		} else {
			BeatRootAdapter beatRoot = new BeatRootAdapter(FILENAME, null);
			ce = new ChordExtractor(c, FILENAME, beatRoot);
		}

		File labFile = new File(LAB_FILENAME);
		if (labFile.exists()) {
			LabFileReader labReader = new LabFileReader(labFile);
			LabSimilarity sim = new LabSimilarity(labReader.getChords(),
					labReader.getTimestamps(), ce.getChords(), ce.getOriginalBeatTimes());
			LOG.info("Overlap measure: " + sim.getOverlapMeasure());
		}
		
		int startOffset = ce.getStartNoteOffsetInSemitonesFromF0();
		Visualizer.visualizeChords(ce.getChords(), ce.getOriginalBeatTimes(), FILENAME, startOffset);
		
		String wavFileName = FILENAME.substring(FILENAME.lastIndexOf(File.separator) + 1);
		String labFileName = wavFileName.substring(0, wavFileName.lastIndexOf(".")) + PathConstants.EXT_LAB;
		String csvFileName = wavFileName.substring(0, wavFileName.lastIndexOf(".")) + PathConstants.EXT_CSV;
		double[] beatTimes = ce.getOriginalBeatTimes();
		
		LabFileWriter labWriter = new LabFileWriter(ce.getChords(), beatTimes);
		try {
			labWriter.writeTo(new File(PathConstants.OUTPUT_DIR + labFileName));
		} catch (IOException e) {
			LOG.error("Error when writing lab file", e);
		}
		
		CsvFileWriter csvWriter = new CsvFileWriter(ce.getChords(), beatTimes);
		try {
			csvWriter.writeTo(new File(PathConstants.OUTPUT_DIR + csvFileName));
		} catch (IOException e) {
			LOG.error("Error when writing csv file", e);
		}
	}

}
