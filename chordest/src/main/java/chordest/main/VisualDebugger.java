package chordest.main;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.Harmony;
import chordest.chord.recognition.IChordRecognition;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
import chordest.transform.CQConstants;
import chordest.transform.DiscreteCosineTransform;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.NoteLabelProvider;
import chordest.util.Visualizer;

/**
 * A dedicated class to visualize required steps of chord extraction process.
 * Mimics the real sequence of steps and allows to insert visualization
 * statements in any place without modification of the ChordExtractor.
 * @author Nikolay
 *
 */
public class VisualDebugger {

	private static final Logger LOG = LoggerFactory.getLogger(VisualDebugger.class);
	public static final String TRACK = "N013-M01-T13";
	public static final String FILE = "spectrum8-60-6\\" + TRACK + ".wav.bin";

	private static final Configuration c = new Configuration();

	private static double startTime = 0;
	private static double endTime   = 3;
	
	private static String[] labels;
	private static String[] labels1;

	public static void main(String[] args) {
		SpectrumData sd = SpectrumFileReader.read(FILE);
		getOctaves(sd, 0, 4);
		
//		double[] beats = getBeats(sd);
//		double[][] spectrum = getSpectrumFragment(sd, beats);
		double[] beats = sd.beatTimes;
		double[][] spectrum = sd.spectrum;
		beats = shrinkBeats(beats, sd.framesPerBeat);
		
		labels = NoteLabelProvider.getNoteLabels(sd.startNoteOffsetInSemitonesFromF0, sd.scaleInfo);
		labels1 = NoteLabelProvider.getNoteLabels(sd.startNoteOffsetInSemitonesFromF0, new ScaleInfo(1, 12));
		
		CQConstants cqc = CQConstants.getInstance(sd.samplingRate, sd.scaleInfo, sd.f0, sd.startNoteOffsetInSemitonesFromF0);
		LOG.info("Max window length: " + (cqc.getLongestWindow() * 1.0 / sd.samplingRate));
//		spectrum = DataUtil.smoothHorizontallyMedianAndShrink(spectrum, 1, sd.framesPerBeat);
		spectrum = DataUtil.smoothHorizontallyMedianAndShrink(spectrum, c.process.medianFilterWindow, sd.framesPerBeat);
		spectrum = DataUtil.toLogSpectrum(spectrum, c.process.crpLogEta);
		visualizeSpectrum(sd, spectrum, "Spectrum");
		
		spectrum = DiscreteCosineTransform.doChromaReduction(spectrum, c.process.crpFirstNonZero);
//		visualizeSpectrum(sd, spectrum, "Reduced");
		double[][] selfSim = DataUtil.getSelfSimilarity(spectrum);
//		selfSim = DataUtil.removeDissimilar(selfSim, c.process.selfSimilarityTheta);
		selfSim = DataUtil.removeDissimilar(selfSim, 0.10);
//		Visualizer.visualizeSelfSimilarity(selfSim, beats);
		spectrum = DataUtil.smoothWithSelfSimilarity(spectrum, selfSim);
		visualizeSpectrum(sd, spectrum, "After self-sim");
		
		double[][] chroma = DataUtil.reduce(spectrum, sd.scaleInfo.octaves);
		chroma = DataUtil.toSingleOctave(chroma, 12);
		visualizeSpectrum(sd, chroma, "Chroma");
		
		Note startNote = Note.byNumber(sd.startNoteOffsetInSemitonesFromF0);
		ITemplateProducer producer = new TemplateProducer(startNote);
		IChordRecognition first = new TemplatesRecognition(producer, null);
		Chord[] chords = first.recognize(chroma, new ScaleInfo(1, 12));
		
		double[] noChordness = DataUtil.getNochordness(spectrum, sd.scaleInfo.octaves);
		for (int i = 0; i < noChordness.length; i++) {
			if (noChordness[i] < 0.0015) {
				chords[i] = Chord.empty();
			}
		}
//		Visualizer.visualizeXByTimeDistribution(noChordness, beats);
		
//		printChords(chords, beats);
		chords = Harmony.smoothUsingHarmony(chroma, chords, new ScaleInfo(1, 12), producer);
		printChords(chords, beats);
//		printTemplate(Chord.major(Note.C));
//		printTemplate(Chord.minor(Note.C));
	}

	private static double[] getBeats(double[] beats) {
		if (startTime > endTime) {
			throw new RuntimeException("startTime must be < than endTime");
		}
		int start = 0;
		while (beats[start] < startTime) {
			start++;
		}
		if (start > 0) { start--; }
		int end = start;
		while (beats[end] < endTime && end < beats.length - 1) {
			end++;
		}
		return ArrayUtils.subarray(beats, start, end);
	}

	private static void visualizeSpectrum(SpectrumData sd, double[][] spectrum, String label) {
		double[] beats = shrinkBeats(sd.beatTimes, c.spectrum.framesPerBeat);
		double[] localBeats = getBeats(beats);
		int startIndex = Arrays.binarySearch(beats, localBeats[0]);
		int endIndex = startIndex + localBeats.length;
		double[][] localSpectrum = ArrayUtils.subarray(spectrum, startIndex, endIndex);
		for (int i = 0; i < localBeats.length; i++) {
			localBeats[i] -= startTime;
		}
		if (localSpectrum[0].length == 12) {
			Visualizer.visualizeSpectrum(localSpectrum, localBeats, labels1, label);
		} else {
			Visualizer.visualizeSpectrum(localSpectrum, localBeats, labels, label);
		}
	}

	private static double[] shrinkBeats(double[] beats, int step) {
		int l = beats.length;
		int newLength = l % step == 0 ? l / step : l / step + 1;
		double[] result = new double[newLength];
		for (int i = 0; i < l; i += step) {
			result[i / step] = beats[i];
		}
		return result;
	}

	private static void printChords(Chord[] chords, double[] beats) {
		double[] localBeats = getBeats(beats);
		int startIndex = Arrays.binarySearch(beats, localBeats[0]);
		int endIndex = startIndex + localBeats.length;
		for (int i = startIndex; i < endIndex; i++) {
			LOG.info(String.format("%f: %s", beats[i], chords[i]));
		}
	}

	private static void printTemplate(Chord chord) {
		double[] array = new TemplateProducer(Note.C).getTemplateFor(chord);
		DataUtil.scaleTo01(array);
		System.out.println(Arrays.toString(array));
	}

	public static void getOctaves(SpectrumData sd, int from, int to) {
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
