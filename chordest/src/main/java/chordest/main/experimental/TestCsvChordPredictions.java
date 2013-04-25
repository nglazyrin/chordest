package chordest.main.experimental;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Note;
import chordest.util.PathConstants;

public class TestCsvChordPredictions extends AbstractTestRecognizeFromCsv {

	private static final Logger LOG = LoggerFactory.getLogger(TestCsvChordPredictions.class);
	private static String CSV_DIRECTORY = PathConstants.CSV_DIR + "svm" + PathConstants.SEP;

	public static void main(String[] args) {
		TestCsvChordPredictions tsv = new TestCsvChordPredictions();
		tsv.recognizeFromCsv();
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

	@Override
	public String getCsvDirectory() {
		return CSV_DIRECTORY;
	}

	@Override
	public Chord[] recognize(File csvFile) {
		return readChords(csvFile);
	}

}
