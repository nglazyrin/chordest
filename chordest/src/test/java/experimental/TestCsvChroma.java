package experimental;

import java.io.File;

import chordest.chord.recognition.TemplatesRecognition;
import chordest.configuration.Configuration;
import chordest.io.csv.CsvSpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.PathConstants;

public class TestCsvChroma extends AbstractTestRecognizeFromCsv {

	private static String CSV_DIRECTORY = PathConstants.CSV_DIR + "encoded" + PathConstants.SEP;

	Configuration c = new Configuration();

	public static void main(String[] args) {
		TestCsvChroma tcc = new TestCsvChroma();
		tcc.recognizeFromCsv();
	}

	@Override
	public String getCsvDirectory() {
		return CSV_DIRECTORY;
	}

	@Override
	public Chord[] recognize(File csvFile) {
		double[][] chroma = new CsvSpectrumFileReader(csvFile).getSpectrum();
		TemplatesRecognition rec = new TemplatesRecognition(Note.byNumber(c.spectrum.offsetFromF0InSemitones));
		return rec.recognize(chroma, new ScaleInfo(1, 12));
	}

}
