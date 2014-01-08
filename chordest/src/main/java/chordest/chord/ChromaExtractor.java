package chordest.chord;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import chordest.configuration.Configuration.ProcessProperties;
import chordest.configuration.Configuration.TemplateProperties;
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
public class ChromaExtractor {

	private final double[] originalBeatTimes;
	private final SpectrumData spectrumData;
	private double[][] chroma;
	private double[] noChordness;
	private Key key;

	public ChromaExtractor(ProcessProperties p, TemplateProperties t,
			ISpectrumDataProvider spectrumProvider) {
		spectrumData = spectrumProvider.getSpectrumData();
		getFirstOctaves(spectrumData, 4);
		
		doChromaExtraction(p, t, spectrumData.spectrum);
		
		double[] tempBeats = DataUtil.toAllBeatTimes(spectrumData.beatTimes, spectrumData.framesPerBeat);
		if (tempBeats[tempBeats.length - 1] < spectrumData.totalSeconds) {
			// append song length as the last beat position
			tempBeats = Arrays.copyOf(tempBeats, tempBeats.length + 1);
			tempBeats[tempBeats.length - 1] = spectrumData.totalSeconds;
			// add a zero chroma vector
			chroma = Arrays.copyOf(chroma, chroma.length + 1);
			chroma[chroma.length - 1] = new double[12];
			// and mark the last beat as having no chord
			noChordness = Arrays.copyOf(noChordness, noChordness.length + 1);
			noChordness[noChordness.length - 1] = 0;
		}
		originalBeatTimes = tempBeats;
	}

	/**
	 * Preserves first <code>octaves</code> in SpectrumData and removes others.
	 * ScaleInfo is also corrected. No changes are saved to disk.
	 * @param sd
	 * @param octaves
	 */
	private void getFirstOctaves(SpectrumData sd, int octaves) {
		if (octaves > sd.scaleInfo.octaves) {
			throw new IllegalArgumentException(String.format(
					"Spectrum contains %s octaves, but %s are required", sd.scaleInfo.octaves, octaves));
		} else if (octaves == sd.scaleInfo.octaves) {
			return;
		}
		sd.scaleInfo = new ScaleInfo(octaves, sd.scaleInfo.notesInOctave);
		int newComponents = sd.scaleInfo.getTotalComponentsCount();
		for (int i = 0; i < sd.spectrum.length; i++) {
			sd.spectrum[i] = ArrayUtils.subarray(sd.spectrum[i], 0, newComponents);
		}
	}

	private void doChromaExtraction(final ProcessProperties p, TemplateProperties t, final double[][] spectrum) {
//		double[][] result = DataUtil.shrink(spectrum, 4); // TODO
//		result = DataUtil.smoothHorizontallyMedianAndShrink(result, p.medianFilterWindow, 2);
		double[][] result = DataUtil.smoothHorizontallyMedianAndShrink(spectrum,
				p.medianFilterWindow, spectrumData.framesPerBeat);
		result = DataUtil.toLogSpectrum(result, p.crpLogEta);
//		result = DataUtil.filterHorizontal3(result);
//		result = DataUtil.removeShortLines(result, 9);
		result = DiscreteCosineTransform.doChromaReduction(result, p.crpFirstNonZero);
		result = doSelfSimSmooth(result, p.selfSimilarityTheta);
		toChromaAndNoChordness(t, result, spectrumData.scaleInfo.octaves);
	}

	private double[][] doSelfSimSmooth(final double[][] spectrum, double theta) {
//		double[][] result = spectrum;
//		result = DataUtil.reduce(spectrum, 4);
		
		double[][] selfSim = DataUtil.getSelfSimilarity(spectrum);
		selfSim = DataUtil.removeDissimilar(selfSim, theta);
		double[][] result = DataUtil.smoothWithSelfSimilarity(spectrum, selfSim);
		return result;
	}

	private void toChromaAndNoChordness(TemplateProperties t, final double[][] sp, int octaves) {
		double[][] sp12 = DataUtil.reduce(sp, octaves);
		chroma = DataUtil.toSingleOctave(sp12, 12);

		Note startNote = Note.byNumber(spectrumData.startNoteOffsetInSemitonesFromF0);
		key = KeyExtractor.getKey(sp12, startNote);
		
		noChordness = DataUtil.getNochordness(sp, octaves);
	}

	public double[] getOriginalBeatTimes() {
		return originalBeatTimes;
	}

	public Note getStartNote() {
		return Note.byNumber(spectrumData.startNoteOffsetInSemitonesFromF0);
	}

	public double[][] getChroma() {
		return chroma;
	}

	public double[] getNoChordness() {
		return noChordness;
	}

	public Key getKey() {
		return key;
	}

}
