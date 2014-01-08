package chordest.main.experimental;

import java.io.File;

import chordest.chord.templates.TemplateProducer;
import chordest.chord.tonnetz.PureTonnetzRecognition;
import chordest.configuration.Configuration;
import chordest.io.csv.CsvSpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.util.PathConstants;

/**
 * Treats the values from .csv file as Tonnetz representation of spectrum.
 * Recognizes the chord by this representation and calculates recognition
 * quality.
 * @author Nikolay
 *
 */
public class TestCsvTonnetz extends AbstractTestRecognizeFromCsv {

	private static String CSV_DIRECTORY = PathConstants.CSV_DIR + "encoded" + PathConstants.SEP;

	private static Configuration c = new Configuration();

	public static void main(String[] args) {
		TestCsvTonnetz tct = new TestCsvTonnetz();
		tct.recognizeFromCsv();
	}

	@Override
	public String getCsvDirectory() {
		return CSV_DIRECTORY;
	}

	@Override
	public Chord[] recognize(File csvFile) {
		double[][] tonnetz = new CsvSpectrumFileReader(csvFile).getSpectrum();
		PureTonnetzRecognition rec = new PureTonnetzRecognition(
				new TemplateProducer(Note.byNumber(c.spectrum.offsetFromF0InSemitones), c.template));
		return rec.recognize(tonnetz);
	}

}
