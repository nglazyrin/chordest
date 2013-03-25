package chordest.io.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Note;

public class CsvBassFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(CsvSpectrumFileReader.class);
	private final Note[] bass;
	
	public CsvBassFileReader(File csv) {
		try (Scanner scanner = new Scanner(csv);) {
			scanner.useLocale(Locale.ENGLISH);
			String line = scanner.nextLine();
			String[] notes = line.split(",");
			bass = new Note[notes.length];
			for (int i = 0; i < notes.length; i++) {
				bass[i] = Chord.N.equals(notes[i]) ? null : Note.valueOf(notes[i].replace('#', 'D'));
			}
			LOG.info("Bass notes were read from " + csv.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (InputMismatchException e) {
			throw new IllegalArgumentException(e);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public Note[] getBass() {
		return bass;
	}

}
