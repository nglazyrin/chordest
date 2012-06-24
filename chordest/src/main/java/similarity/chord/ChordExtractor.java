package similarity.chord;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import similarity.beat.BeatRootAdapter;
import similarity.chord.recognition.TemplatesRecognition;
import similarity.spectrum.SpectrumData;
import similarity.spectrum.SpectrumFileReader;
import similarity.spectrum.SpectrumFileWriter;
import similarity.transform.CQConstants;
import similarity.transform.PooledTransformer;
import similarity.transform.ScaleInfo;
import similarity.util.DataUtil;
import similarity.util.MapUtil;
import similarity.util.NoteLabelProvider;
import similarity.util.TuningFrequencyFinder;
import similarity.wave.WaveReader;
import uk.co.labbookpages.WavFile;
import uk.co.labbookpages.WavFileException;
import utils.Visualizer;

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
	
	public static final ScaleInfo scaleInfo = new ScaleInfo(4, 60);
	
	private final double[] originalBeatTimes;
	private final double[] beatTimes;
	private final Chord[] chords;
	private final double[][] spectrum;
	private final double totalSeconds;
	private final int startNoteOffsetInSemitonesFromF0;
	private final Mode mode;

	public ChordExtractor(String wavFilePath, String beatFilePath, String spectrumFilePath) {
		BeatRootAdapter beatRoot = new BeatRootAdapter(wavFilePath, beatFilePath);
		originalBeatTimes = beatRoot.getBeatTimes();
		beatTimes = expand(originalBeatTimes, 3);
		LOG.debug("Transforms: " + beatTimes.length);
		
		SpectrumData data = readWaveFile(wavFilePath, spectrumFilePath, scaleInfo, beatTimes);
		double[][] result = data.spectrum;
		totalSeconds = data.totalSeconds;
		startNoteOffsetInSemitonesFromF0 = data.startNoteOffsetInSemitonesFromF0;
		
		DataUtil.scaleTo01(result);
		
//		String[] labels = NoteLabelProvider.getNoteLabels(startNoteOffsetInSemitonesFromF0, scaleInfo);
//		String[] labels1 = NoteLabelProvider.getNoteLabels(startNoteOffsetInSemitonesFromF0, new ScaleInfo(1, 12));
//		Visualizer.visualizeSpectrum(result, beatTimes, labels, "Spectrum as is");
		
//		double[][] whitened = DataUtil.smoothHorizontally(result, 16);
//		whitened = DataUtil.whitenSpectrum(result, scaleInfo.getNotesInOctaveCount());
//		whitened = DataUtil.removeShortLines(whitened, 8);
//		Visualizer.visualizeSpectrum(whitened, beatTimes, labels, "Whitened spectrum after short lines removal");

		result = DataUtil.smoothHorizontallyMedian(result, 17); // step 1
		result = DataUtil.filterHorizontal3(result);			// step 2
//		result = DataUtil.removeShortLines(result, 20);			// step 2
		
//		Visualizer.visualizeSpectrum(result, beatTimes, labels, "Prewitt filtered spectrum");

//		double[] e = DataUtil.getSoundEnergyByFrequencyDistribution(result);
//		Visualizer.visualizeXByFrequencyDistribution(e, scaleInfo, startNoteOffsetInSemitonesFromF0);

		result = DataUtil.shrink(result, 8);
//		whitened = DataUtil.shrink(whitened, 8);
		
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Prewitt after shrink");
		result = DataUtil.smoothHorizontallyMedian(result, 3);	// step 3
//		whitened = DataUtil.smoothHorizontally(whitened, 2);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Prewitt after smooth 2");

		double[][] pcp = DataUtil.toPitchClassProfiles(result, scaleInfo.getNotesInOctaveCount());
//		double[][] pcp = DataUtil.toPitchClassProfiles(whitened, scaleInfo.getNotesInOctaveCount());
		pcp = DataUtil.reduceTo12Notes(pcp);
//		Visualizer.visualizeSpectrum(pcp, originalBeatTimes, labels1, "PCP");
		double[][] rp = DataUtil.getSelfSimilarity(pcp);		// step 6
		rp = DataUtil.getRecurrencePlot(rp);					// step 6
//		Visualizer.visualizeSelfSimilarity(rp, originalBeatTimes);
		pcp = DataUtil.smoothWithRecurrencePlot(pcp, rp);		// step 6
//		Visualizer.visualizeSpectrum(pcp, originalBeatTimes, labels1, "PCP with RP");

		spectrum = pcp;

		Note startNote = Note.byNumber(startNoteOffsetInSemitonesFromF0);
//		mode = Mode.recognizeMode(getTonalProfile(pcp, 0, pcp.length), startNote);
		mode = null;
		TemplatesRecognition second = new TemplatesRecognition(startNote, mode);
//		chords = second.recognize(spectrum, scaleInfo);
		chords = second.recognize(spectrum, new ScaleInfo(1, 12));
	}

	private SpectrumData readWaveFile(String filename, String spectrumFileName,
			ScaleInfo scaleInfo, double[] beatTimes2) {
		SpectrumData result = new SpectrumData();
		result.beatTimes = beatTimes2;
		result.scaleInfo = scaleInfo;
		result.startNoteOffsetInSemitonesFromF0 = -33; // 0 = A5, -33 = C3
		result.wavFilePath = filename;
		WavFile wavFile = null;
		try {
			wavFile = WavFile.openWavFile(new File(filename));
			result.samplingRate = (int) wavFile.getSampleRate();
			int frames = (int) wavFile.getNumFrames();
			result.totalSeconds = frames * 1.0 / result.samplingRate;
			
			SpectrumData serialized = SpectrumFileReader.read(spectrumFileName);
			if (result.equalsIgnoreSpectrumAndF0(serialized)) {
				LOG.info("Spectrum was read from " + spectrumFileName);
				return serialized;
			} else {
				result.f0 = TuningFrequencyFinder.getTuningFrequency(filename);
//				result.f0 = CQConstants.F0_DEFAULT;
				CQConstants cqConstants = CQConstants.getInstance(result.samplingRate,
						result.scaleInfo, result.f0, result.startNoteOffsetInSemitonesFromF0);
				int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
				double shift = windowSize / (result.samplingRate * 2.0);
				for (int i = 0; i < result.beatTimes.length; i++) {
					result.beatTimes[i] -= shift;
				}
				WaveReader reader = new WaveReader(wavFile, result.beatTimes, windowSize);
				PooledTransformer transformer = new PooledTransformer(
						reader, result.beatTimes.length, result.scaleInfo, cqConstants);
				result.spectrum = transformer.run();
				if (spectrumFileName != null) {
					SpectrumFileWriter.write(spectrumFileName, result);
				}
				return result;
			}
		} catch (WavFileException e) {
			throw new IllegalArgumentException("Error when reading wave file " + filename, e);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error when reading wave file " + filename, e);
		} catch (InterruptedException e) {
			throw new IllegalArgumentException("Error when reading wave file " + filename, e);
		} finally {
			if (wavFile != null) { try {
				wavFile.close();
			} catch (IOException ignore) {} }
		}
	}

	public double[] getOriginalBeatTimes() {
		return ArrayUtils.add(originalBeatTimes, totalSeconds);
//		return originalBeatTimes;
	}

	public double[] getBeatTimes() {
		return beatTimes;
	}

	public Chord[] getChords() {
		return chords;
	}

	public Mode getMode() {
		return mode;
	}

	public double[][] getSpectrum() {
		return spectrum;
	}

	public double getTotalSeconds() {
		return totalSeconds;
	}

	public int getStartNoteOffsetInSemitonesFromF0() {
		return startNoteOffsetInSemitonesFromF0;
	}

	/**
	 * For each pair of successive elements in an array inserts arithmetic 
	 * mean of them in the middle. This operation is performed for a specified
	 * number of times. The resulting array has length 2^times * (L - 1) + 1
	 * where L is the length of the source array. 
	 * @param array
	 * @param times
	 * @return
	 */
	private double[] expand(double[] array, int times) {
		double[] temp = array;
		for (int i = 0; i < times; i++) {
			temp = DataUtil.expandBeatTimes(temp);
		}
		return temp;
	}

}
