package experimental;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordListsComparison;
import chordest.chord.ComparisonAccumulator;
import chordest.io.csv.CsvFileWriter;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.main.Roundtrip;
import chordest.model.Chord;
import chordest.spectrum.SpectrumData;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public abstract class AbstractTestRecognizeFromCsv {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractTestRecognizeFromCsv.class);
	private static final Logger SIM_LOG = LoggerFactory.getLogger("Similarity");

	private static String BIN_DIRECTORY = "spectrum8" + PathConstants.SEP;
	private static final String TEST_FILE_LIST = "work" + PathConstants.SEP + "all_files1.txt";

	private static ComparisonAccumulator acc = new ComparisonAccumulator();
//	private static Configuration c = new Configuration();

	public abstract String getCsvDirectory();

	public abstract Chord[] recognize(File csvFile);

	public void recognizeFromCsv() {
		List<String> tracklist = TracklistCreator.readTrackList(TEST_FILE_LIST);
		SIM_LOG.info("name,overlap,effective_length,full_length");
		for (String item : tracklist) {
			String track = StringUtils.substringAfterLast(item, PathConstants.SEP);
			String binFile = BIN_DIRECTORY + track;
			String csvFile = getCsvDirectory() + track + PathConstants.EXT_CSV;
			String labFile = PathConstants.LAB_DIR + StringUtils.replace(track, PathConstants.EXT_WAV + PathConstants.EXT_BIN, PathConstants.EXT_LAB);
			double rco = doChordRecognition(binFile, csvFile, labFile);
			LOG.info(binFile + " RCO: " + rco);
		}

		LOG.info("AOR: " + acc.getAOR());
		LOG.info("WAOR: " + acc.getWAOR());
		int top = 40;
		LOG.info(String.format("Errors TOP-%d:", top));
		List<Entry<String, Double>> errors = acc.getErrors();
		for (int i = 0; i < top && i < errors.size(); i++) {
			LOG.info(String.format("%s: %f", errors.get(i).getKey(), errors.get(i).getValue()));
		}
	}

	private double doChordRecognition(String binFile, String csvFile, String expectedLab) {
		SpectrumData sd = SpectrumFileReader.read(binFile);
		Chord[] chords = recognize(new File(csvFile));
		double[] beatTimes = new double[sd.beatTimes.length / sd.framesPerBeat + 1];
		for (int i = 0; i < beatTimes.length; i++) {
			beatTimes[i] = sd.beatTimes[sd.framesPerBeat * i];
		}
		beatTimes = Arrays.copyOf(beatTimes, beatTimes.length + 1);
		if (beatTimes.length > 2) {
			double beatLength = beatTimes[1] - beatTimes[0];
			double lastSound = beatTimes[beatTimes.length - 3] + beatLength;
			beatTimes[beatTimes.length - 1] = beatTimes[beatTimes.length - 2];
			beatTimes[beatTimes.length - 2] = lastSound;
		}
		LabFileWriter lw = new LabFileWriter(chords, beatTimes);
		File temp = null;
		try {
			temp = File.createTempFile("chordest", PathConstants.EXT_LAB);
			lw.writeTo(temp);
		} catch (IOException e) {
			LOG.error("Error when saving temporary lab file", e);
			System.exit(-1);
		}
		
		LabFileReader labReaderExpected = new LabFileReader(new File(expectedLab));
		LabFileReader labReaderActual = new LabFileReader(temp);
		
		String track = StringUtils.substringAfterLast(expectedLab, PathConstants.SEP);
		String csvFileName = StringUtils.replace(track, PathConstants.EXT_LAB, PathConstants.EXT_CSV);
		Roundtrip.write(new CsvFileWriter(labReaderActual.getChords(), labReaderActual.getTimestamps()), Roundtrip.CSV_ACTUAL_DIR + csvFileName);
		Roundtrip.write(new CsvFileWriter(labReaderExpected.getChords(), labReaderExpected.getTimestamps()), Roundtrip.CSV_EXPECTED_DIR + csvFileName);
		
		ChordListsComparison cmp = new ChordListsComparison(labReaderExpected.getChords(),
				labReaderExpected.getTimestamps(), labReaderActual.getChords(), labReaderActual.getTimestamps());
		acc.append(cmp);
		
		SIM_LOG.info(expectedLab.substring(4).replace(',', '_').replace('\\', '/') + "," + 
				cmp.getOverlapMeasure() + "," + cmp.getTotalSeconds() + "," + sd.totalSeconds);
		
		return cmp.getOverlapMeasure();
	}

}
