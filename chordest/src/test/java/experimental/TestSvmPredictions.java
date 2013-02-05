package experimental;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordListsComparison;
import chordest.chord.ComparisonAccumulator;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class TestSvmPredictions {

	private static final Logger LOG = LoggerFactory.getLogger(TestSvmPredictions.class);

	private static String BIN_DIRECTORY = "spectrum8" + PathConstants.SEP;
	private static String CSV_DIRECTORY = PathConstants.CSV_DIR + "svm" + PathConstants.SEP;

	private static ComparisonAccumulator acc = new ComparisonAccumulator();

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList("work" + PathConstants.SEP + "all_files1.txt");
		for (String item : tracklist) {
			String track = StringUtils.substringAfterLast(item, PathConstants.SEP);
			String binFile = BIN_DIRECTORY + track;
			String csvFile = CSV_DIRECTORY + track + PathConstants.EXT_CSV;
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

	private static double doChordRecognition(String binFile, String csvFile, String expectedLab) {
		SpectrumData sd = SpectrumFileReader.read(binFile);
		Chord[] chords = readChords(new File(csvFile));
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
		ChordListsComparison cmp = new ChordListsComparison(labReaderExpected.getChords(),
				labReaderExpected.getTimestamps(), labReaderActual.getChords(), labReaderActual.getTimestamps());
		acc.append(cmp);
		
		return cmp.getOverlapMeasure();
	}

	private static Chord[] readChords(File csvFile) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(csvFile);
			scanner.useLocale(Locale.ENGLISH);
			String line = scanner.nextLine();
			String[] chordNotes = line.split(",");
			Chord[] result = new Chord[chordNotes.length];
			for (int i = 0; i < chordNotes.length; i++) {
				String notesDashed = chordNotes[i];
				if ("N".equals(notesDashed)) {
					result[i] = Chord.empty();
				} else {
					String[] noteNames = notesDashed.split("-");
					Note[] notes = new Note[noteNames.length];
					for (int j = 0; j < noteNames.length; j++) {
						notes[j] = Note.valueOf(noteNames[j].replace('#', 'D'));
					}
					result[i] = new Chord(notes);
				}
			}
			LOG.info("Chords were read from " + csvFile.getAbsolutePath());
			return result;
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (InputMismatchException e) {
			throw new IllegalArgumentException(e);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

}
