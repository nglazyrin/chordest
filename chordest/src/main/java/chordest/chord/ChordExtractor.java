package chordest.chord;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.labbookpages.WavFile;
import uk.co.labbookpages.WavFileException;
import chordest.beat.BeatRootAdapter;
import chordest.chord.recognition.TemplatesRecognition;
import chordest.properties.Configuration;
import chordest.spectrum.SpectrumData;
import chordest.spectrum.SpectrumFileReader;
import chordest.transform.CQConstants;
import chordest.transform.PooledTransformer;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.TuningFrequencyFinder;
import chordest.wave.WaveReader;

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
	private final double[] expandedBeatTimes;
	private final double totalSeconds;
	private final int startNoteOffsetInSemitonesFromF0;
	private final SpectrumData spectrum;
	private Chord[] chords;
	private Key key;

	public ChordExtractor(Configuration c, String wavFilePath, BeatRootAdapter beatRoot) {
		originalBeatTimes = beatRoot.getBeatTimes();
		expandedBeatTimes = DataUtil.makeMoreFrequent(originalBeatTimes, c.spectrum.framesPerBeat);
		LOG.debug("Transforms: " + expandedBeatTimes.length);
		
		spectrum = readSpectrum(c, wavFilePath, expandedBeatTimes);
		totalSeconds = spectrum.totalSeconds;
		startNoteOffsetInSemitonesFromF0 = spectrum.startNoteOffsetInSemitonesFromF0;
		doChordExtraction(c);
	}

	public ChordExtractor(Configuration c, String spectrumFilePath) {
		spectrum = readSpectrum(spectrumFilePath);
		totalSeconds = spectrum.totalSeconds;
		startNoteOffsetInSemitonesFromF0 = spectrum.startNoteOffsetInSemitonesFromF0;
		expandedBeatTimes = restoreBeatTimes();
		
		int framesPerBeat = c.spectrum.framesPerBeat;
		originalBeatTimes = new double[expandedBeatTimes.length / framesPerBeat + 1];
		for (int i = 0; i < originalBeatTimes.length; i++) {
			originalBeatTimes[i] = expandedBeatTimes[framesPerBeat * i];
		}
		
		doChordExtraction(c);
	}

	/**
	 * Beat times in spectrum are really the start positions of constant-Q
	 * transform analysis windows. But those windows are centered at the
	 * real beat time positions, which we need to restore. So we add half of
	 * the longest constant-Q window to each position
	 * @return
	 */
	private double[] restoreBeatTimes() {
		double[] result = new double[spectrum.beatTimes.length];
		double shift = getWindowsShift(spectrum);
		for (int i = 0; i < result.length; i++) {
			result[i] = spectrum.beatTimes[i] + shift;
		}
		return result;
	}

	private double getWindowsShift(SpectrumData data) {
		CQConstants cqConstants = CQConstants.getInstance(data.samplingRate,
				data.scaleInfo, data.f0, data.startNoteOffsetInSemitonesFromF0);
		int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
		double shift = windowSize / (data.samplingRate * 2.0);
		return shift;
	}

	private void doChordExtraction(Configuration c) {
		double[][] result = spectrum.spectrum;
		DataUtil.scaleTo01(result);
		
//		String[] labels = NoteLabelProvider.getNoteLabels(startNoteOffsetInSemitonesFromF0, scaleInfo);
//		String[] labels1 = NoteLabelProvider.getNoteLabels(startNoteOffsetInSemitonesFromF0, new ScaleInfo(1, 12));
//		Visualizer.visualizeSpectrum(result, beatTimes, labels, "Spectrum as is");
		
//		double[][] whitened = DataUtil.smoothHorizontally(result, 16);
//		whitened = DataUtil.whitenSpectrum(result, scaleInfo.getNotesInOctaveCount());
//		whitened = DataUtil.removeShortLines(whitened, 8);
//		Visualizer.visualizeSpectrum(whitened, beatTimes, labels, "Whitened spectrum after short lines removal");

		result = DataUtil.smoothHorizontallyMedian(result, c.process.medianFilter1Window); // step 1
		result = DataUtil.filterHorizontal3(result);			// step 2
//		result = DataUtil.removeShortLines(result, 20);			// step 2
		
//		Visualizer.visualizeSpectrum(result, beatTimes, labels, "Prewitt filtered spectrum");

//		double[] e = DataUtil.getSoundEnergyByFrequencyDistribution(result);
//		Visualizer.visualizeXByFrequencyDistribution(e, scaleInfo, startNoteOffsetInSemitonesFromF0);

		result = DataUtil.shrink(result, c.spectrum.framesPerBeat);
//		whitened = DataUtil.shrink(whitened, c.spectrum.framesPerBeat);
		
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Prewitt after shrink");
		result = DataUtil.smoothHorizontallyMedian(result, c.process.medianFilter2Window);	// step 3
//		whitened = DataUtil.smoothHorizontally(whitened, 2);
//		Visualizer.visualizeSpectrum(result, originalBeatTimes, labels, "Prewitt after smooth 2");

		double[][] pcp = DataUtil.toPitchClassProfiles(result, c.spectrum.notesPerOctave);
//		double[][] pcp = DataUtil.toPitchClassProfiles(whitened, c.spectrum.notesPerOctave);
		pcp = DataUtil.reduceTo12Notes(pcp);
//		Visualizer.visualizeSpectrum(pcp, originalBeatTimes, labels1, "PCP");
		double[][] rp = DataUtil.getSelfSimilarity(pcp);		// step 6
		rp = DataUtil.getDiagonalMatrix(rp, c.process.recurrencePlotTheta, c.process.recurrencePlotMinLength);					// step 6
//		Visualizer.visualizeSelfSimilarity(rp, originalBeatTimes);
		pcp = DataUtil.smoothWithRecurrencePlot(pcp, rp);		// step 6
//		Visualizer.visualizeSpectrum(pcp, originalBeatTimes, labels1, "PCP with RP");

		Note startNote = Note.byNumber(startNoteOffsetInSemitonesFromF0);
//		key = Key.recognizeKey(getTonalProfile(pcp, 0, pcp.length), startNote);
		key = null;
		TemplatesRecognition second = new TemplatesRecognition(startNote, key);
//		chords = second.recognize(pcp, scaleInfo);
		chords = second.recognize(pcp, new ScaleInfo(1, 12));
	}

	private SpectrumData readSpectrum(Configuration c, String waveFileName,
			 double[] expandedBeatTimes) {
		SpectrumData result = new SpectrumData();
		result.beatTimes = expandedBeatTimes;
		result.scaleInfo = new ScaleInfo(c.spectrum.octaves, c.spectrum.notesPerOctave);
		result.startNoteOffsetInSemitonesFromF0 = c.spectrum.offsetFromF0InSemitones;
		result.wavFilePath = waveFileName;
		WavFile wavFile = null;
		try {
			wavFile = WavFile.openWavFile(new File(waveFileName));
			result.samplingRate = (int) wavFile.getSampleRate();
			int frames = (int) wavFile.getNumFrames();
			result.totalSeconds = frames * 1.0 / result.samplingRate;
			
			result.f0 = TuningFrequencyFinder.getTuningFrequency(waveFileName, c.process.threadPoolSize);
//				result.f0 = CQConstants.F0_DEFAULT;
			CQConstants cqConstants = CQConstants.getInstance(result.samplingRate,
					result.scaleInfo, result.f0, result.startNoteOffsetInSemitonesFromF0);
			int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
			// need to make windows centered at the beat positions, so shift them to the left
			double shift = getWindowsShift(result);
			for (int i = 0; i < result.beatTimes.length; i++) {
				result.beatTimes[i] -= shift;
			}
			WaveReader reader = new WaveReader(wavFile, result.beatTimes, windowSize);
			PooledTransformer transformer = new PooledTransformer(
					reader, c.process.threadPoolSize, result.beatTimes.length, result.scaleInfo, cqConstants);
			result.spectrum = transformer.run();
		} catch (WavFileException e) {
			LOG.error("Error when reading wave file " + waveFileName, e);
		} catch (IOException e) {
			LOG.error("Error when reading wave file " + waveFileName, e);
		} catch (InterruptedException e) {
			LOG.error("Error when reading wave file " + waveFileName, e);
		} finally {
			if (wavFile != null) { try {
				wavFile.close();
			} catch (IOException e) {
				LOG.error("Error when closing file " + waveFileName, e);
			} }
		}
		return result;
	}

	private SpectrumData readSpectrum(String spectrumFileName) {
		SpectrumData serialized = SpectrumFileReader.read(spectrumFileName);
		if (serialized != null) {
			LOG.info("Spectrum was read from " + spectrumFileName);
		}
		return serialized;
	}

	public double[] getOriginalBeatTimes() {
		return ArrayUtils.add(originalBeatTimes, totalSeconds);
//		return originalBeatTimes;
	}

	public double[] getBeatTimes() {
		return expandedBeatTimes;
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

	public double getTotalSeconds() {
		return totalSeconds;
	}

	public int getStartNoteOffsetInSemitonesFromF0() {
		return startNoteOffsetInSemitonesFromF0;
	}

}
