package chordest.main.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import chordest.chord.Harmony;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.io.csv.CsvSpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
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
		
		List<Integer> noChords = new ArrayList<Integer>();
		for (int i = 0; i < chroma.length; i++) {
			double[] t = chroma[i];
			double max = 0;
			for (int j = 0; j < t.length; j++) {
				max = Math.max(Math.abs(t[j]), max);
			}
			if (max < 4.5) {
				noChords.add(i);
			}
		}
		DataUtil.scaleEachTo01(chroma);
		double[][] selfSim = DataUtil.getSelfSimilarity(chroma);
		selfSim = DataUtil.removeDissimilar(selfSim, 0.1);
		chroma = DataUtil.smoothWithSelfSimilarity(chroma, selfSim);
		
//		String[] labels = NoteLabelProvider.getNoteLabels(c.spectrum.offsetFromF0InSemitones, new ScaleInfo(1, 12));
//		double[] beatTimes = new double[chroma.length];
//		for (int i = 0; i < beatTimes.length; i++) { beatTimes[i] = i; }
//		Visualizer.visualizeSpectrum(chroma, beatTimes, labels, "Chroma");
		
		Note startNote = Note.byNumber(c.spectrum.offsetFromF0InSemitones);
		ITemplateProducer producer = new TemplateProducer(startNote);
		TemplatesRecognition rec = new TemplatesRecognition(producer);
		Chord[] temp = rec.recognize(chroma, new ScaleInfo(1, 12));
		for (Integer idx : noChords) {
			temp[idx] = Chord.empty();
		}
		return Harmony.smoothUsingHarmony(chroma, temp, new ScaleInfo(1, 12), producer);
//		return new Viterbi(producer).decode(chroma);
//		return temp;
	}

}
