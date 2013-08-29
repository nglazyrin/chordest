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

/**
 * This class encapsulates all the chord extraction logic. When the
 * constructor finishes, all the extracted information is available. It
 * includes the positions of all the beats in the file (obtained from a
 * beat tracker), the array of recognized chords, the array of chroma vectors,
 * the recognized key and the resulting spectrum as a double[][].
 * @author Nikolay
 *
 */
public class ChordExtractor {

	private final double[] originalBeatTimes;
	private final SpectrumData spectrumData;
	private final Chord[] chords;
	private double[][] chroma;
	private Key key;

	public ChordExtractor(ProcessProperties p, ISpectrumDataProvider spectrumProvider) {
		spectrumData = spectrumProvider.getSpectrumData();
		getFirstOctaves(spectrumData, 4);
		originalBeatTimes = DataUtil.toAllBeatTimes(spectrumData.beatTimes, spectrumData.framesPerBeat);
		
		chords = doChordExtraction(p, spectrumData.spectrum);
	}

	/**
	 * Preservs first <code>octaves</code> in SpectrumData and removes others.
	 * ScaleInfo is also corrected. No changes are saved to disk.
	 * @param sd
	 * @param octaves
	 */
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
//		double[][] result = DataUtil.smoothHorizontallyMedian(spectrum, p.medianFilterWindow);
//		result = DataUtil.shrink(result, spectrumData.framesPerBeat);
		double[][] result = DataUtil.smoothHorizontallyMedianAndShrink(spectrum,
				p.medianFilterWindow, spectrumData.framesPerBeat);
		result = DataUtil.toLogSpectrum(result, p.crpLogEta);
//		result = DataUtil.filterHorizontal3(result);
//		result = DataUtil.removeShortLines(result, 9);
		return doTemplateMatching(doChromaReductionAndSelfSimSmooth(result, p.crpFirstNonZero, p.selfSimilarityTheta), spectrumData.scaleInfo.octaves);
	}

	private double[][] doChromaReductionAndSelfSimSmooth(final double[][] spectrum,
			int crpNZ,  double theta) {
		double[][] result = DiscreteCosineTransform.doChromaReduction(spectrum, crpNZ);
//		double[][] result = spectrum;
//		result = DataUtil.reduce(spectrum, 4);
		double[][] selfSim = DataUtil.getSelfSimilarity(result);
		selfSim = DataUtil.removeDissimilar(selfSim, theta);
		result = DataUtil.smoothWithSelfSimilarity(result, selfSim);
		return result;
	}

	private Chord[] doTemplateMatching(final double[][] sp, int octaves) {
		double[][] sp12 = DataUtil.reduce(sp, octaves);
		chroma = DataUtil.toSingleOctave(sp12, 12);

//		key = Key.recognizeKey(getTonalProfile(pcp, 0, pcp.length), startNote);
		key = null;
		Note startNote = Note.byNumber(spectrumData.startNoteOffsetInSemitonesFromF0);
		ITemplateProducer producer = new TemplateProducer(startNote);
		IChordRecognition first = new TemplatesRecognition(producer, key);
		Chord[] temp = first.recognize(chroma, new ScaleInfo(1, 12));
		
		double[] noChordness = DataUtil.getNochordness(sp, octaves);
		for (int i = 0; i < noChordness.length; i++) {
			if (noChordness[i] < 0.0014) { // 0.00125 for 5 octaves
				temp[i] = Chord.empty();
			}
		}
		
//		return temp;
		return Harmony.smoothUsingHarmony(chroma, temp, new ScaleInfo(1, 12), producer);
//		return new Viterbi(producer).decode(chroma);
	}

	public double[] getOriginalBeatTimes() {
		return originalBeatTimes;
	}

	public Chord[] getChords() {
		return chords;
	}

	public double[][] getChroma() {
		return chroma;
	}

	public Key getKey() {
		return key;
	}

	public SpectrumData getSpectrum() {
		return spectrumData;
	}

}
