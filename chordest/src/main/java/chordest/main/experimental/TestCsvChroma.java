package chordest.main.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import chordest.chord.Harmony;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.io.csv.CsvSpectrumFileReader;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.main.VisualDebugger;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
import chordest.transform.DiscreteCosineTransform;
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
		
		double[] noChordness = new double[chroma.length];
		for (int i = 0; i < chroma.length; i++) {
			double[] t = chroma[i];
			double max = 0;
			for (int j = 0; j < t.length; j++) {
				max = Math.max(Math.abs(t[j]), max);
			}
			if (max < 0.1) { // TODO: was 4.5 or 1.5
				noChordness[i] = 0;
			} else {
				noChordness[i] = 1;
			}
		}
		
//		int octaves = 4;
//		double[][] binSpectrum = getBinSpectrum(csvFile, octaves);
//		noChordness = DataUtil.getNochordness(binSpectrum, octaves);
//		double[][] chroma2 = getBinChroma(binSpectrum, octaves);
//		chroma = chroma2;
//		for (int i = 0; i < chroma.length; i++) {
//			chroma[i] = DataUtil.add(chroma[i], chroma2[i]);
//		}
		
		DataUtil.scaleEachTo01(chroma);
		
//		double[][] selfSim = DataUtil.getSelfSimilarity(chroma); // TODO
//		selfSim = DataUtil.removeDissimilar(selfSim, c.process.selfSimilarityTheta);
//		chroma = DataUtil.smoothWithSelfSimilarity(chroma, selfSim);
		
		Note startNote = Note.byNumber(c.spectrum.offsetFromF0InSemitones);
		ITemplateProducer producer = new TemplateProducer(startNote);
		TemplatesRecognition rec = new TemplatesRecognition(producer);
		Chord[] temp = rec.recognize(chroma, new ScaleInfo(1, 12));
		for (int i = 0; i < noChordness.length; i++) {
			if (noChordness[i] < 0.0015) {
				temp[i] = Chord.empty();
			}
		}
		return Harmony.smoothUsingHarmony(chroma, temp, new ScaleInfo(1, 12), producer);
//		return new Viterbi(producer).decode(chroma);
//		return temp;
	}

//	private double[][] getBinChroma(double[][] spectrum, int octaves) {
//		double[][] chroma = DataUtil.reduce(spectrum, octaves);
//		chroma =  DataUtil.toSingleOctave(chroma, 12);
//		DataUtil.scaleEachTo01(chroma);
//		return chroma;
//	}
//
//	private double[][] getBinSpectrum(File csvFile, int octaves) {
//		String binFileName = StringUtils.substringBeforeLast(csvFile.getName(), PathConstants.EXT_CSV);
//		SpectrumData sd = SpectrumFileReader.read("spectrum8-60-6\\" + binFileName);
//		VisualDebugger.getOctaves(sd, 0, octaves);
//		double[][] spectrum = sd.spectrum;
//		spectrum = DataUtil.smoothHorizontallyMedianAndShrink(spectrum, c.process.medianFilterWindow, sd.framesPerBeat);
//		spectrum = DataUtil.toLogSpectrum(spectrum);
//		spectrum = DiscreteCosineTransform.doChromaReduction(spectrum, c.process.crpFirstNonZero);
//		double[][] selfSim = DataUtil.getSelfSimilarity(spectrum);
//		selfSim = DataUtil.removeDissimilar(selfSim, c.process.selfSimilarityTheta);
//		spectrum = DataUtil.smoothWithSelfSimilarity(spectrum, selfSim);
//		return spectrum;
//	}

}
