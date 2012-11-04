package chordest.chord;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.templates.TemplatesRecognition;
import chordest.configuration.Configuration;
import chordest.model.Chord;
import chordest.model.Key;
import chordest.model.Note;
import chordest.spectrum.ISpectrumDataProvider;
import chordest.spectrum.SpectrumData;
import chordest.transform.CQConstants;
import chordest.transform.DiscreteCosineTransform;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.NoteLabelProvider;
import chordest.util.Visualizer;

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

	private static final Logger LOG = LoggerFactory.getLogger(ChordExtractor.class);
	
	private final double[] originalBeatTimes;
	private final SpectrumData spectrum;
	private final Chord[] chords;
	private Key key;

	private final String[] labels;
	private final String[] labels1;

	public ChordExtractor(Configuration c, ISpectrumDataProvider spectrumProvider) {
		spectrum = spectrumProvider.getSpectrumData();
		double[] expandedBeatTimes = restoreBeatTimes();
		
		int framesPerBeat = c.spectrum.framesPerBeat;
		originalBeatTimes = new double[expandedBeatTimes.length / framesPerBeat + 1];
		for (int i = 0; i < originalBeatTimes.length; i++) {
			originalBeatTimes[i] = expandedBeatTimes[framesPerBeat * i];
		}
		
		int offset = spectrum.startNoteOffsetInSemitonesFromF0;
		labels = NoteLabelProvider.getNoteLabels(offset, spectrum.scaleInfo);
		labels1 = NoteLabelProvider.getNoteLabels(offset, new ScaleInfo(1, 12));
		
		chords = doChordExtraction(c);
	}

	/**
	 * Beat times in spectrum are really the start positions of constant-Q
	 * transform analysis windows. But those windows are centered at the
	 * real beat time positions, which we need to restore. So we add half of
	 * the longest constant-Q window to each position
	 * @return
	 */
	private double[] restoreBeatTimes() {
		final double[] result = new double[spectrum.beatTimes.length];
		final double shift = getWindowsShift();
		for (int i = 0; i < result.length; i++) {
			result[i] = spectrum.beatTimes[i] + shift;
		}
		return result;
	}

	private double getWindowsShift() {
		CQConstants cqConstants = CQConstants.getInstance(spectrum.samplingRate,
				spectrum.scaleInfo, spectrum.f0, spectrum.startNoteOffsetInSemitonesFromF0);
		int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
		double shift = windowSize / (spectrum.samplingRate * 2.0);
		return shift;
	}

	private Chord[] doChordExtraction(final Configuration c) {
		double[][] result = spectrum.spectrum;
		
//		result = DataUtil.whitenSpectrum(result, spectrum.scaleInfo.getNotesInOctaveCount());
		result = DataUtil.smoothHorizontallyMedian(result, c.process.medianFilterWindow);
		result = DataUtil.shrink(result, c.spectrum.framesPerBeat);
		
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Spectrum as is");
//		XYDataset dataset = DatasetUtil.toXYDataset(originalBeatTimes, DataUtil.getSpectralFlatness(result));
//		JFreeChartUtils.visualize("Spectral variance", "Time", "Value", dataset);
//		double[] e = DataUtil.getSoundEnergyByFrequencyDistribution(result);
//		Visualizer.visualizeXByFrequencyDistribution(e, scaleInfo, spectrum.startNoteOffsetInSemitonesFromF0);

		result = DataUtil.toLogSpectrum(result);

//		return doTemplateMatching(c, doChromaReductionAndSelfSimSmooth(c, result, 30, 10));
//		return doTemplateMatching(c, doChromaReductionAndSelfSimSmooth(c, result, 20, 20));
//		return doTemplateMatching(c, doChromaReductionAndSelfSimSmooth(c, result, 10, 10));
		return doTemplateMatching(c, doChromaReductionAndSelfSimSmooth(c, result, 30, c.process.crpFirstNonZero));
	}

	private double[][] doChromaReductionAndSelfSimSmooth(final Configuration c,
			final double[][] spectrum, int simNZ, int procNZ) {
		double[][] red = DiscreteCosineTransform.doChromaReduction(spectrum, simNZ);
//		Visualizer.visualizeSpectrum(red, originalBeatTimes, labels, "Reduced " + simNZ);
		double[][] selfSim = DataUtil.getSelfSimilarity(red);
		selfSim = DataUtil.removeDissimilar(selfSim, c.process.selfSimilarityTheta);
//		Visualizer.visualizeSelfSimilarity(selfSim, originalBeatTimes);
		
		double[][] result = DiscreteCosineTransform.doChromaReduction(spectrum, procNZ);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Reduced " + procNZ);
		result = DataUtil.smoothWithSelfSimilarity(result, selfSim);
		return result;
	}

	private Chord[] doTemplateMatching(final Configuration c, final double[][] sp) {
		double[][] result = DataUtil.toSingleOctave(sp, c.spectrum.notesPerOctave);
		double[][] chromas = DataUtil.reduceTo12Notes(result);

//		key = Key.recognizeKey(getTonalProfile(pcp, 0, pcp.length), startNote);
		key = null;
		Note startNote = Note.byNumber(spectrum.startNoteOffsetInSemitonesFromF0);
		TemplatesRecognition first = new TemplatesRecognition(startNote, key);
		Chord[] temp = first.recognize(chromas, new ScaleInfo(1, 12));
		LOG.info("Normalized diff: " + first.getDiffNormalized());
		
//		Map<Chord, double[]> newTemplates = TemplatesSmoother.smoothTemplates(chromas, temp);
//		for (Entry<Chord, double[]> entry : newTemplates.entrySet()) {
//			LOG.info(entry.getKey().toString() + " - " + Arrays.toString(entry.getValue()));
//		}
//		TemplatesRecognition second = new TemplatesRecognition(startNote, newTemplates.keySet(),
//				new SimpleTemplateProducer(newTemplates));
//		temp = second.recognize(chromas, new ScaleInfo(1, 12));
		
		return Harmony.smoothUsingHarmony(chromas, temp, new ScaleInfo(1, 12), startNote);
	}

	public double[] getOriginalBeatTimes() {
		return ArrayUtils.add(originalBeatTimes, spectrum.totalSeconds);
	}

	public Chord[] getChords() {
		return chords;
	}

	public Key getKey() {
		return key;
	}

	public SpectrumData getSpectrum() {
		return spectrum;
	}

}
