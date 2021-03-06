package chordest.spectrum;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import chordest.beat.BeatRootBeatTimesProvider;
import chordest.beat.IBeatTimesProvider;
import chordest.beat.VampBeatTimesProvider;
import chordest.configuration.Configuration;
import chordest.configuration.Configuration.PreProcessProperties;
import chordest.configuration.Configuration.SpectrumProperties;
import chordest.transform.CQConstants;
import chordest.transform.DummyConstantQTransform;
import chordest.transform.FFTTransformWrapper;
import chordest.transform.ITransform;
import chordest.transform.PooledTransformer;
import chordest.transform.PooledTransformer.ITransformProvider;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.QUtil;
import chordest.util.TuningFrequencyFinder;
import chordest.wave.Buffer;
import chordest.wave.WaveFileInfo;
import chordest.wave.WaveReader;

public class WaveFileSpectrumDataProvider implements ISpectrumDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(WaveFileSpectrumDataProvider.class);

	private final double[] beatTimes;

	private final SpectrumData spectrumData;

	private final boolean useConstantQTransform = true;

	public WaveFileSpectrumDataProvider(final String waveFileName, String beatFileName, Configuration c) {
		IBeatTimesProvider provider = null;
//		try {
//			provider = new BeatRootBeatTimesProvider(waveFileName);
//		} catch (Throwable e) {
//			provider = null;
//		}
		if (provider == null) {
			try {
				provider = new VampBeatTimesProvider(waveFileName, beatFileName, c.pre);
			} catch (Throwable e) {
				e.printStackTrace();
				// last try to provide a sequence of beats
				provider = new IBeatTimesProvider() {
					@Override
					public double[] getBeatTimes() {
						return DataUtil.generateDefaultBeats(waveFileName, 0.5);
					}
				};
			}
		}
		beatTimes = provider.getBeatTimes();
		spectrumData = readSpectrum(c.spectrum, waveFileName, beatFileName, beatTimes, c.pre);
	}

	public WaveFileSpectrumDataProvider(String waveFileName, String beatFileName, Configuration c, IBeatTimesProvider provider) {
		double[] temp = provider.getBeatTimes();
		if (temp.length == 0) {
			try {
				temp = new VampBeatTimesProvider(waveFileName, beatFileName, c.pre).getBeatTimes();
			} catch (Throwable e) {
				// last try to provide a sequence of beats
				temp = DataUtil.generateDefaultBeats(waveFileName, 0.5);
			}
		}
		beatTimes = temp;
		spectrumData = readSpectrum(c.spectrum, waveFileName, beatFileName, beatTimes, c.pre);
	}

	@Override
	public SpectrumData getSpectrumData() {
		return spectrumData;
	}

	public double[] getBeatTimes() {
		return beatTimes;
	}

	private SpectrumData readSpectrum(SpectrumProperties s, String waveFileName,
			 String beatFileName, double[] beatTimes, PreProcessProperties p) {
		final SpectrumData result = new SpectrumData();
		result.beatTimes = DataUtil.makeMoreFrequent(beatTimes, s.framesPerBeat);
		result.scaleInfo = new ScaleInfo(s.octaves, s.notesPerOctave);
		result.startNoteOffsetInSemitonesFromF0 = s.offsetFromF0InSemitones;
		result.framesPerBeat = s.framesPerBeat;
		
		WaveFileInfo wfi = new WaveFileInfo(waveFileName);
		if (wfi.exception != null) {
			LOG.error("Error when reading wave file " + waveFileName, wfi.exception);
		}
		result.samplingRate = wfi.samplingRate;
		result.totalSeconds = wfi.seconds;
		if (useConstantQTransform && p.estimateTuningFrequency) {
//			double[] tuningBeatTimes = shiftBeatsLeft(beatTimes,
//					getWindowsShiftForTuning(s.notesPerOctave, s.offsetFromF0InSemitones, result.samplingRate));
			result.f0 = TuningFrequencyFinder.getTuningFrequency(waveFileName, beatTimes, s.threadPoolSize);
//			result.f0 = new VampTuningFrequencyFinder(waveFileName, beatFileName, p).getTuningFrequency();
		} else {
			result.f0 = CQConstants.F0_DEFAULT;
		}
		
		boolean REDUCE_FIRST = false; // TODO
		final CQConstants cqConstants = CQConstants.getInstance(result.samplingRate,
				result.scaleInfo, result.f0, result.startNoteOffsetInSemitonesFromF0, REDUCE_FIRST);
		if (REDUCE_FIRST) {
			result.scaleInfo = new ScaleInfo(result.scaleInfo.octaves, 12);
		}
		int windowSize;
		ITransformProvider provider;
		if (useConstantQTransform) {
			windowSize = cqConstants.getLongestWindow() + 1;
			provider = new ITransformProvider() {
				@Override
				public ITransform getTransform(Buffer buffer, CountDownLatch latch) {
					return new DummyConstantQTransform(buffer, result.scaleInfo, latch, cqConstants);
				}
			};
		} else {
			windowSize = 32768;
			provider = new ITransformProvider() {
				@Override
				public ITransform getTransform(Buffer buffer, CountDownLatch latch) {
					return new FFTTransformWrapper(buffer, latch, cqConstants);
				}
			};
		}
		
		// need to make windows centered at the beat positions, so shift them to the left
		final double[] windowBeginnings = shiftBeatsLeft(result.beatTimes,
				getWindowsShift(cqConstants, s.beatTimesDelay * 0.001, result.samplingRate));
		final WaveReader reader = new WaveReader(new File(waveFileName), windowBeginnings, windowSize);
		final PooledTransformer transformer = new PooledTransformer(
				reader, s.threadPoolSize, result.beatTimes.length, provider);
		try {
			result.spectrum = transformer.run();
		} catch (InterruptedException e) {
			LOG.error("Error when reading wave file " + waveFileName, e);
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

	private double getWindowsShift(CQConstants cqConstants, double beatTimesDelay, int samplingRate) {
		int windowSize = cqConstants.getLongestWindow() + 1; // the longest window
		double shift = windowSize / (samplingRate * 2.0);
		return shift - beatTimesDelay;
	}

	private static double getWindowsShiftForTuning(int notesInOctave, int offsetInSemitonesFromF0,
			int samplingRate, double beatTimesDelay) {
		double Q = QUtil.calculateQ(notesInOctave);
		double minFreq = 440 * Math.pow(2, offsetInSemitonesFromF0 / 12.0);
		double window = Q * samplingRate / minFreq;
		double shift = window / (samplingRate * 2.0);
		return shift - beatTimesDelay;
	}

}
