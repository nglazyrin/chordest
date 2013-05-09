package chordest.chord;

import org.apache.commons.lang3.ArrayUtils;

import chordest.chord.recognition.IChordRecognition;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration.ProcessProperties;
import chordest.model.Chord;
import chordest.model.Key;
import chordest.model.Note;
import chordest.spectrum.ISpectrumDataProvider;
import chordest.spectrum.SpectrumData;
import chordest.transform.DiscreteCosineTransform;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.NoteLabelProvider;
import chordest.util.Visualizer;
import chordest.util.Viterbi;

/**
 * This class incapsulates all the chord extraction logic. All you need is to
 * pass a name of the wave file as a constructor parameter. When the
 * constructor finishes, all the extracted information is available. It
 * includes the positions of all the beats in the file (extracted with
 * BeatRoot), the positions that were used for recognition (which are
 * essentially the original beat positions supplemented with intermediate
 * points), the array of recognized chords, the recognized mode and the
 * resulting spectrum as a double[][]. 
 * @author Nikolay
 *
 */
public class ChordExtractor {

	private final double[] originalBeatTimes;
	private final SpectrumData spectrumData;
	private final Chord[] chords;
	private Key key;
	private IExternalProcessor externalProcessor;

	private final String[] labels;
	private final String[] labels1;

	public ChordExtractor(ProcessProperties p, ISpectrumDataProvider spectrumProvider) {
		this(p, spectrumProvider, null);
	}

	public ChordExtractor(ProcessProperties p, ISpectrumDataProvider spectrumProvider,
			IExternalProcessor ex) {
		this.externalProcessor = ex;
		spectrumData = spectrumProvider.getSpectrumData();
		getFirstOctaves(spectrumData, 4);
		int framesPerBeat = spectrumData.framesPerBeat;
		originalBeatTimes = new double[spectrumData.beatTimes.length / framesPerBeat + 1];
		for (int i = 0; i < originalBeatTimes.length; i++) {
			originalBeatTimes[i] = spectrumData.beatTimes[framesPerBeat * i];
		}
		
		int offset = spectrumData.startNoteOffsetInSemitonesFromF0;
		labels = NoteLabelProvider.getNoteLabels(offset, spectrumData.scaleInfo);
		labels1 = NoteLabelProvider.getNoteLabels(offset, new ScaleInfo(1, 12));
		
//		Visualizer.visualizeSpectrum(spectrumData.spectrum, spectrumData.beatTimes, labels, "Spectrum as is");
		chords = doChordExtraction(p, spectrumData.spectrum);
	}

	private void getFirstOctaves(SpectrumData sd, int octaves) {
		if (octaves > sd.scaleInfo.octaves) {
			throw new IllegalArgumentException("Too many octaves to get");
		}
		sd.scaleInfo = new ScaleInfo(octaves, sd.scaleInfo.notesInOctave);
		int newComponents = sd.scaleInfo.getTotalComponentsCount();
		for (int i = 0; i < sd.spectrum.length; i++) {
			sd.spectrum[i] = ArrayUtils.subarray(sd.spectrum[i], 0, newComponents);
		}
	}

	private Chord[] doChordExtraction(final ProcessProperties p, final double[][] spectrum) {
//		Visualizer.visualizeXByFrequencyDistribution(e, scaleInfo, spectrum.startNoteOffsetInSemitonesFromF0);
		
		double[][] result = DataUtil.smoothHorizontallyMedian(spectrum, p.medianFilterWindow);
		result = DataUtil.shrink(result, spectrumData.framesPerBeat);
		result = DataUtil.toLogSpectrum(result);
		
//		result = DataUtil.whitenSpectrum(result, spectrumData.scaleInfo.notesInOctave);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Original spectrum");
//		if (externalProcessor != null) {
//			result = externalProcessor.process(result);
//			Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Modified spectrum");
//		}

		return doTemplateMatching(doChromaReductionAndSelfSimSmooth(result, p.crpFirstNonZero, p.selfSimilarityTheta), spectrumData.scaleInfo.octaves);
	}

	private double[][] doChromaReductionAndSelfSimSmooth(final double[][] spectrum,
			int simNZ,  double theta) {
//		double[][] result = DataUtil.reduce(spectrum, 4);
		double[][] result = DiscreteCosineTransform.doChromaReduction(spectrum, simNZ);
//		double[][] result = spectrum;
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Reduced");
		double[][] selfSim = DataUtil.getSelfSimilarity(result);
		selfSim = DataUtil.removeDissimilar(selfSim, theta);
//		Visualizer.visualizeSelfSimilarity(selfSim, originalBeatTimes);
		
		result = DataUtil.smoothWithSelfSimilarity(result, selfSim);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Reduced + self-sim");
		return result;
	}

	private Chord[] doTemplateMatching(final double[][] sp, int octaves) {
		double[][] sp12 = DataUtil.reduce(sp, octaves);
		double[][] chromas = DataUtil.toSingleOctave(sp12, 12);

//		key = Key.recognizeKey(getTonalProfile(pcp, 0, pcp.length), startNote);
		key = null;
		Note startNote = Note.byNumber(spectrumData.startNoteOffsetInSemitonesFromF0);
		ITemplateProducer producer = new TemplateProducer(startNote);
		IChordRecognition first = new TemplatesRecognition(producer, key);
		Chord[] temp = first.recognize(chromas, new ScaleInfo(1, 12));
		
		double[] noChordness = DataUtil.getNochordness(sp, octaves);
		for (int i = 0; i < noChordness.length; i++) {
			if (noChordness[i] < 0.0015) {
				temp[i] = Chord.empty();
			}
		}
//		Visualizer.visualizeXByTimeDistribution(noChordness, originalBeatTimes);
		
		return Harmony.smoothUsingHarmony(chromas, temp, new ScaleInfo(1, 12), producer);
//		return new Viterbi(producer).decode(chromas);
	}

	public double[] getOriginalBeatTimes() {
		return ArrayUtils.add(originalBeatTimes, spectrumData.totalSeconds);
	}

	public Chord[] getChords() {
		return chords;
	}

	public Key getKey() {
		return key;
	}

	public SpectrumData getSpectrum() {
		return spectrumData;
	}

	public static interface IExternalProcessor {
		public double[][] process(double[][] data);
	}

}
