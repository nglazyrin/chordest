package experimental;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import chordest.chord.Harmony;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.recognition.TemplatesWithBassRecognition;
import chordest.configuration.Configuration;
import chordest.io.csv.CsvSpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.NoteLabelProvider;
import chordest.util.PathConstants;
import chordest.util.Visualizer;

public class TestCsvChromaBass extends AbstractTestRecognizeFromCsv {

	private static String BASS_DIRECTORY = PathConstants.CSV_DIR + "bass" + PathConstants.SEP;

	private static String CSV_DIRECTORY = PathConstants.CSV_DIR + "encoded" + PathConstants.SEP;

	Configuration c = new Configuration();

	public static void main(String[] args) {
		TestCsvChromaBass tcc = new TestCsvChromaBass();
		tcc.recognizeFromCsv();
	}

	@Override
	public String getCsvDirectory() {
		return CSV_DIRECTORY;
	}

	@Override
	public Chord[] recognize(File csvFile) {
		String fileLastName = csvFile.toPath().getFileName().toString();
		Path bassPath = FileSystems.getDefault().getPath(BASS_DIRECTORY, fileLastName);
		
		double[][] chroma = new CsvSpectrumFileReader(csvFile).getSpectrum();
//		Note[] bass = new CsvBassFileReader(bassPath.toFile()).getBass();
		double[][] bassChroma = new CsvSpectrumFileReader(bassPath.toFile()).getSpectrum();
		DataUtil.scaleEachTo01(bassChroma);
//		DataUtil.scaleEachTo01(chroma);
//		for (int i = 0; i < bassChroma.length; i++) {
//			for (int j = 0; j < bassChroma[i].length; j++) {
//				bassChroma[i][j] = bassChroma[i][j] < 1 ? 0 : 1;
//			}
//		}
		
		// add bass chroma to main chroma
//		double[][] temp1 = new double[chroma.length][];
//		for (int i = 0; i < temp1.length; i++) {
//			temp1[i] = DataUtil.add(chroma[i], bassChroma[i]);
//		}
//		chroma = temp1;
		
		// visualize bass chroma
//		double[] m = new double[bassChroma.length];
//		for (int i = 0; i < m.length; i++) { m[i] = i; }
//		Visualizer.visualizeSpectrum(bassChroma, m, NoteLabelProvider.getNoteLabels(-33, new ScaleInfo(1, 12)), "Bass");
//		Visualizer.visualizeSpectrum(chroma, m, NoteLabelProvider.getNoteLabels(-33, new ScaleInfo(1, 12)), "Chroma");
		
		// transform bass chroma to sequence of bass notes
//		Note[] bass = new Note[bassChroma.length];
//		for (int i = 0; i < bass.length; i++) {
//			double[] array = bassChroma[i];
//			int maxPos = 0;
//			double max = array[0];
//			for (int j = 1; j < array.length; j++) {
//				if (array[j] > max) {
//					max = array[j];
//					maxPos = j;
//				}
//			}
//			bass[i] = Note.C.withOffset(maxPos);
//		}
		
		// self similarity
//		DataUtil.scaleEachTo01(chroma);
//		double[][] selfSim = DataUtil.getSelfSimilarity(chroma);
//		selfSim = DataUtil.removeDissimilar(selfSim, 0.1);
//		chroma = DataUtil.smoothWithSelfSimilarity(chroma, selfSim);
		
//		selfSim = DataUtil.getSelfSimilarity(bassChroma);
//		selfSim = DataUtil.removeDissimilar(selfSim, 0.1);
//		bassChroma = DataUtil.smoothWithSelfSimilarity(bassChroma, selfSim);
		
		Note startNote = Note.byNumber(c.spectrum.offsetFromF0InSemitones);
		TemplatesWithBassRecognition rec = new TemplatesWithBassRecognition(startNote, bassChroma);
//		TemplatesRecognition rec = new TemplatesRecognition(startNote);
		Chord[] temp = rec.recognize(chroma, new ScaleInfo(1, 12));
		return Harmony.smoothUsingHarmony(chroma, temp, new ScaleInfo(1, 12), startNote);
//		return temp;
	}

}
