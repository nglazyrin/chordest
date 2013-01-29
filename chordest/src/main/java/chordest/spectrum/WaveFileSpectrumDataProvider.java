package chordest.spectrum;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.labbookpages.WavFile;
import uk.co.labbookpages.WavFileException;
import chordest.beat.BeatRootBeatTimesProvider;
import chordest.configuration.Configuration.SpectrumProperties;
import chordest.transform.CQConstants;
import chordest.transform.PooledTransformer;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.TuningFrequencyFinder;
import chordest.wave.WaveReader;

public class WaveFileSpectrumDataProvider implements ISpectrumDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(WaveFileSpectrumDataProvider.class);

	private final SpectrumData spectrumData;

	public WaveFileSpectrumDataProvider(String waveFileName, SpectrumProperties s) {
		double[] beatTimes = new BeatRootBeatTimesProvider(waveFileName).getBeatTimes();
		double[] expandedBeatTimes = DataUtil.makeMoreFrequent(beatTimes, s.framesPerBeat);
		spectrumData = readSpectrum(s, waveFileName, expandedBeatTimes);
	}

	public WaveFileSpectrumDataProvider(String waveFileName, SpectrumProperties s, double[] expandedBeatTimes) {
		spectrumData = readSpectrum(s, waveFileName, expandedBeatTimes);
	}

	@Override
	public SpectrumData getSpectrumData() {
		return spectrumData;
	}

	private SpectrumData readSpectrum(SpectrumProperties s, String waveFileName,
			 double[] expandedBeatTimes) {
		SpectrumData result = new SpectrumData();
		result.beatTimes = expandedBeatTimes;
		result.scaleInfo = new ScaleInfo(s.octaves, s.notesPerOctave);
		result.startNoteOffsetInSemitonesFromF0 = s.offsetFromF0InSemitones;
		result.framesPerBeat = s.framesPerBeat;
		result.f0 = TuningFrequencyFinder.getTuningFrequency(waveFileName, s.threadPoolSize);
//		result.f0 = CQConstants.F0_DEFAULT;
		WavFile wavFile = null;
		try {
			wavFile = WavFile.openWavFile(new File(waveFileName));
			result.samplingRate = (int) wavFile.getSampleRate();
			result.totalSeconds = wavFile.getNumFrames() * 1.0 / result.samplingRate;
			
			CQConstants cqConstants = CQConstants.getInstance(result.samplingRate,
					result.scaleInfo, result.f0, result.startNoteOffsetInSemitonesFromF0);
			int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
			// need to make windows centered at the beat positions, so shift them to the left
			double[] windowBeginnings = shiftBeatsLeft(result.beatTimes, getWindowsShift(result));
			WaveReader reader = new WaveReader(wavFile, windowBeginnings, windowSize);
			PooledTransformer transformer = new PooledTransformer(
					reader, s.threadPoolSize, result.beatTimes.length, result.scaleInfo, cqConstants);
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

	/**
	 * Constant-Q analysis windows need to be centered at the beat positions,
	 * so we shift beat positions to the left to get analysis windows
	 * beginnings.
	 * @param beats
	 * @param shift
	 * @return
	 */
	private static double[] shiftBeatsLeft(double[] beats, double shift) {
		double[] windowBeginnings = new double[beats.length];
		for (int i = 0; i < beats.length; i++) {
			windowBeginnings[i] = beats[i] - shift;
		}
		return windowBeginnings;
	}

	private static double getWindowsShift(SpectrumData data) {
		CQConstants cqConstants = CQConstants.getInstance(data.samplingRate,
				data.scaleInfo, data.f0, data.startNoteOffsetInSemitonesFromF0);
		int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
		double shift = windowSize / (data.samplingRate * 2.0);
		return shift;
	}

}
