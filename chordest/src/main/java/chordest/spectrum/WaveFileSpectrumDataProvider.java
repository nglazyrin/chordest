package chordest.spectrum;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.labbookpages.WavFile;
import uk.co.labbookpages.WavFileException;

import chordest.beat.BeatRootBeatTimesProvider;
import chordest.configuration.Configuration;
import chordest.transform.CQConstants;
import chordest.transform.PooledTransformer;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;
import chordest.util.TuningFrequencyFinder;
import chordest.wave.WaveReader;

public class WaveFileSpectrumDataProvider implements ISpectrumDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(WaveFileSpectrumDataProvider.class);

	private final SpectrumData spectrumData;

	public WaveFileSpectrumDataProvider(String waveFileName, Configuration c) {
		double[] beatTimes = new BeatRootBeatTimesProvider(waveFileName).getBeatTimes();
		double[] expandedBeatTimes = DataUtil.makeMoreFrequent(beatTimes, c.spectrum.framesPerBeat);
		spectrumData = readSpectrum(c, waveFileName, expandedBeatTimes);
	}

	@Override
	public SpectrumData getSpectrumData() {
		return spectrumData;
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
//			result.f0 = CQConstants.F0_DEFAULT;
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

	private double getWindowsShift(SpectrumData data) {
		CQConstants cqConstants = CQConstants.getInstance(data.samplingRate,
				data.scaleInfo, data.f0, data.startNoteOffsetInSemitonesFromF0);
		int windowSize = cqConstants.getWindowLengthForComponent(0) + 1; // the longest window
		double shift = windowSize / (data.samplingRate * 2.0);
		return shift;
	}

}
