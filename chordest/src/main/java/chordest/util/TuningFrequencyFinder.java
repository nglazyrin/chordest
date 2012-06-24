package chordest.util;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.transform.CQConstants;
import chordest.transform.PooledTransformer;
import chordest.transform.ScaleInfo;
import chordest.wave.WaveReader;

import uk.co.labbookpages.WavFile;
import uk.co.labbookpages.WavFileException;

public class TuningFrequencyFinder {
	
	private static final Logger LOG = LoggerFactory.getLogger(TuningFrequencyFinder.class);
	
	public static final int OCTAVES = 4;
	public static final int BINS_PER_NOTE = 10;
	public static final int NOTES_IN_OCTAVE = 12 * BINS_PER_NOTE;
	public static final double BASIC_FREQUENCY = 440;

	public static double getTuningFrequency(String fileName) {
		return getTuningFrequency(fileName, BASIC_FREQUENCY);
	}

	public static double getTuningFrequency(String filename, double basicFrequency) {
		WavFile wavFile = null;
		try {
			wavFile = WavFile.openWavFile(new File(filename));
			int samplingRate = (int) wavFile.getSampleRate();
			int frames = (int) wavFile.getNumFrames();
			double seconds = frames * 1.0 / samplingRate;
			double[] beatTimes = new double[(int) Math.floor(seconds)];
			for (int i = 0; i < beatTimes.length; i++) {
				beatTimes[i] = i;
			}
			
			ScaleInfo scaleInfo = new ScaleInfo(OCTAVES, NOTES_IN_OCTAVE);
			CQConstants cqc = CQConstants.getInstance(samplingRate, scaleInfo, CQConstants.F0_DEFAULT, 0);
			int windowSize = cqc.getWindowLengthForComponent(0) + 1; // the longest window
			
			WaveReader reader = new WaveReader(wavFile, beatTimes, windowSize);
			PooledTransformer transformer = new PooledTransformer(
					reader, beatTimes.length, scaleInfo, cqc);
			double[][] spectrum = transformer.run();
			int[] tunes = new int[BINS_PER_NOTE];
			for (double[] data : spectrum) {
				double[] fold = new double[BINS_PER_NOTE];
				int subnote = 0;
				for (double value : data) {
					fold[subnote++ % BINS_PER_NOTE] += value;
				}
				int maxPos = 0; double max = fold[0];
				for (int i = 1; i < fold.length; i++) {
					if (fold[i] > max) { max = fold[i]; maxPos = i; }
				}
				tunes[maxPos]++;
			}
			int maxPos = 0; int max = tunes[0];
			for (int i = 1; i < tunes.length; i++) {
				if (tunes[i] > max) { max = tunes[i]; maxPos = i; }
			}
			if (maxPos >= BINS_PER_NOTE/2) {
				maxPos = maxPos - BINS_PER_NOTE;
			}
			double power = maxPos / (12.0 * BINS_PER_NOTE);
			double result = Math.pow(2.0, power) * basicFrequency;
			LOG.info(String.format("Tuning frequency: %f Hz", result));
			return result;
		} catch (WavFileException e) {
			LOG.error("Error when reading wave file", e);
		} catch (IOException e) {
			LOG.error("Error when reading wave file", e);
		} catch (InterruptedException e) {
			LOG.error("Error when performing transforms", e);
		} finally {
			if (wavFile != null) { try {
				wavFile.close();
			} catch (IOException ignore) {} }
		}
		return BASIC_FREQUENCY;
	}

}
