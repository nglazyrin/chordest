package chordest.chord;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		int framesPerBeat = spectrumData.framesPerBeat;
		originalBeatTimes = new double[spectrumData.beatTimes.length / framesPerBeat + 1];
		for (int i = 0; i < originalBeatTimes.length; i++) {
			originalBeatTimes[i] = spectrumData.beatTimes[framesPerBeat * i];
		}
		
		int offset = spectrumData.startNoteOffsetInSemitonesFromF0;
		labels = NoteLabelProvider.getNoteLabels(offset, spectrumData.scaleInfo);
		labels1 = NoteLabelProvider.getNoteLabels(offset, new ScaleInfo(1, 12));
		
//		Visualizer.visualizeSpectrum(spectrumData.spectrum, originalBeatTimes, labels, "Spectrum as is");
		chords = doChordExtraction(p, spectrumData.spectrum);
	}

	private Chord[] doChordExtraction(final ProcessProperties p, final double[][] spectrum) {
		double[][] result = spectrum;
//		double[] e = DataUtil.getSoundEnergyByFrequencyDistribution(result);
//		Visualizer.visualizeXByFrequencyDistribution(e, scaleInfo, spectrum.startNoteOffsetInSemitonesFromF0);
		
		result = DataUtil.smoothHorizontallyMedian(result, p.medianFilterWindow);
		result = DataUtil.shrink(result, spectrumData.framesPerBeat);
		result = DataUtil.toLogSpectrum(result);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Original spectrum");
		if (externalProcessor != null) {
			result = externalProcessor.process(result);
//			Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Modified spectrum");
		}

//		return doTemplateMatching(doChromaReductionAndSelfSimSmooth(result, 30, 10, p.selfSimilarityTheta), spectrumData.scaleInfo.getNotesInOctaveCount());
//		return doTemplateMatching(doChromaReductionAndSelfSimSmooth(result, 20, 20, p.selfSimilarityTheta), spectrumData.scaleInfo.getNotesInOctaveCount());
//		return doTemplateMatching(doChromaReductionAndSelfSimSmooth(result, 10, 10, p.selfSimilarityTheta), spectrumData.scaleInfo.getNotesInOctaveCount());
		return doTemplateMatching(doChromaReductionAndSelfSimSmooth(result, 30, p.crpFirstNonZero, p.selfSimilarityTheta), spectrumData.scaleInfo.getNotesInOctaveCount());
	}

	private double[][] doChromaReductionAndSelfSimSmooth(final double[][] spectrum,
			int simNZ, int procNZ, double theta) {
//		double[][] red = DiscreteCosineTransform.doChromaReduction(spectrum, simNZ);
//		Visualizer.visualizeSpectrum(red, originalBeatTimes, labels, "Reduced " + simNZ);
//		double[][] selfSim = DataUtil.getSelfSimilarity(red);
//		selfSim = DataUtil.removeDissimilar(selfSim, theta);
//		Visualizer.visualizeSelfSimilarity(selfSim, originalBeatTimes);
		
		double[][] result = DiscreteCosineTransform.doChromaReduction(spectrum, procNZ);
//		double[][] result = spectrum;
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Reduced " + procNZ);
//		result = DataUtil.smoothWithSelfSimilarity(result, selfSim);
		return result;
	}

	private Chord[] doTemplateMatching(final double[][] sp, int notesPerOctave) {
		double[][] result = DataUtil.toSingleOctave(sp, notesPerOctave);
		double[][] chromas = DataUtil.reduceTo12Notes(result);

//		key = Key.recognizeKey(getTonalProfile(pcp, 0, pcp.length), startNote);
		key = null;
		Note startNote = Note.byNumber(spectrumData.startNoteOffsetInSemitonesFromF0);
		ITemplateProducer producer = new TemplateProducer(startNote, true);
		IChordRecognition first = new TemplatesRecognition(producer, key);
//		IChordRecognition first = new TonnetzRecognition(producer);
		Chord[] temp = first.recognize(chromas, new ScaleInfo(1, 12));
//		LOG.info("Normalized diff: " + first.getDiffNormalized());
		
//		Map<Chord, double[]> newTemplates = TemplatesSmoother.smoothTemplates(chromas, temp);
//		for (Entry<Chord, double[]> entry : newTemplates.entrySet()) {
//			LOG.info(entry.getKey().toString() + " - " + Arrays.toString(entry.getValue()));
//		}
//		IChordRecognition second = new TemplatesRecognition(newTemplates.keySet(),
//				new SimpleTemplateProducer(newTemplates));
//		temp = second.recognize(chromas, new ScaleInfo(1, 12));
		
		return Harmony.smoothUsingHarmony(chromas, temp, new ScaleInfo(1, 12), producer);
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
