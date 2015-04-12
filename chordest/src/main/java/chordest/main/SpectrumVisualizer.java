package chordest.main;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.transform.DiscreteCosineTransform;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.NoteLabelProvider;
import chordest.util.PathConstants;
import chordest.util.Visualizer;

public class SpectrumVisualizer {

//	public static final String FILE = PathConstants.WAV_DIR + "Beatles/04_-_Beatles_for_Sale/04_-_Rock_and_Roll_Music.wav";
	public static final String TRACK = "love_me_do";
	public static final String FILE = PathConstants.WAV_DIR + TRACK + PathConstants.EXT_WAV;
	public static final String BEAT_FILE_NAME = PathConstants.BEAT_DIR + TRACK + PathConstants.EXT_BEAT;

	public static void main(String[] args) {
		Configuration c = new Configuration();
//		double[] array = new TemplateProducer(Note.C).getTemplateFor(Chord.major(Note.C));
//		DataUtil.scaleTo01(array);
//		System.out.println(Arrays.toString(array));
		WaveFileSpectrumDataProvider p = new WaveFileSpectrumDataProvider(FILE, BEAT_FILE_NAME, c);
		SpectrumData sd = p.getSpectrumData();
		//getOctaves(sd, 1, 3);
		double[][] spectrum = sd.spectrum;
		String[] labels = NoteLabelProvider.getNoteLabels(sd.startNoteOffsetInSemitonesFromF0, sd.scaleInfo);
//		String[] labels1 = NoteLabelProvider.getNoteLabels(sd.startNoteOffsetInSemitonesFromF0, new ScaleInfo(1, 12));
//		Visualizer.visualizeSpectrum(spectrum, sd.beatTimes, labels, "Spectrum");
//		spectrum = DataUtil.whitenSpectrum(spectrum, c.spectrum.notesPerOctave);
//		Visualizer.visualizeSpectrum(spectrum, sd.beatTimes, labels, "Spectrum");
		spectrum = DataUtil.smoothHorizontallyMedian(spectrum, c.process.medianFilterWindow / 2);
//		spectrum = DataUtil.filterHorizontal3(spectrum);
//		spectrum = DataUtil.removeShortLines(spectrum, 8);
//		Visualizer.visualizeSpectrum(spectrum, sd.beatTimes, labels, "Spectrum");
//		spectrum = DataUtil.shrink(spectrum, sd.framesPerBeat);
//		Visualizer.visualizeSpectrum(spectrum, sd.beatTimes, labels, "Spectrum");
//		spectrum = DataUtil.toLogSpectrum(spectrum, c.process.crpLogEta);
//		spectrum = DiscreteCosineTransform.doChromaReduction(spectrum, c.process.crpFirstNonZero);
		Visualizer.visualizeSpectrum(spectrum, sd.beatTimes, labels, "Spectrum");
		DataUtil.scaleEachTo01(spectrum);
		double[][] ss = DataUtil.getSelfSimilarity(spectrum);
		ss = DataUtil.removeDissimilar(ss, c.process.selfSimilarityTheta);
		spectrum = DataUtil.smoothWithSelfSimilarity(spectrum, ss);
		Visualizer.visualizeSpectrum(spectrum, sd.beatTimes, labels, "Spectrum after self-sim");
		Visualizer.visualizeSelfSimilarity(ss, sd.beatTimes);
		
//		labels = new String[190];
//		for (int i = 0; i < labels.length; i ++) {
//			labels[i] = "" + (22050.0 * i / 4096);
//		}
//		Visualizer.visualizeSpectrum(spectrum, sd.beatTimes, labels, "Spectrum");
		
//		double[][] sp12 = DataUtil.reduce(spectrum, 4);
//		double[][] chromas = DataUtil.toSingleOctave(sp12, 12);
//		Visualizer.visualizeSpectrum(chromas, sd.beatTimes, labels1, "Spectrum");
		
//		double[] result = new double[sd.scaleInfo.getTotalComponentsCount()];
//		for (int i = 0; i < spectrum.length; i++) {
//			result = DataUtil.add(result, spectrum[i]);
//		}
//		Visualizer.visualizeXByFrequencyDistribution(result, sd.scaleInfo, sd.startNoteOffsetInSemitonesFromF0);
	}

	private static void getOctaves(SpectrumData sd, int from, int to) {
		if (to > sd.scaleInfo.octaves) {
			throw new IllegalArgumentException("Too many octaves to get");
		}
		sd.scaleInfo = new ScaleInfo(to - from, sd.scaleInfo.notesInOctave);
		int newFrom = from * sd.scaleInfo.notesInOctave;
		int newTo = to * sd.scaleInfo.notesInOctave;
		for (int i = 0; i < sd.spectrum.length; i++) {
			sd.spectrum[i] = ArrayUtils.subarray(sd.spectrum[i], newFrom, newTo);
		}
	}

}
