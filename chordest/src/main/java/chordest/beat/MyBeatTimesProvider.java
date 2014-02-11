package chordest.beat;

import java.awt.Graphics;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.output.WriterOutputStream;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYZDataset;
import org.junit.internal.matchers.Each;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ofai.music.audio.Util;
import uk.co.labbookpages.WavFile;
import chordest.io.beat.Beat2FileReader;
import chordest.io.beat.BeatFileWriter;
import chordest.io.beat.BeatOnlyFileWriter;
import chordest.transform.FFTTransformWrapper;
import chordest.transform.ITransform;
import chordest.transform.PooledTransformer;
import chordest.transform.PooledTransformer.ITransformProvider;
import chordest.util.DatasetUtil;
import chordest.util.Visualizer;
import chordest.wave.Buffer;
import chordest.wave.WaveReader;

public class MyBeatTimesProvider implements IBeatTimesProvider {
	
	private double[] beatTimes;
	private int samplingRate = 0;
	private double[] originalEnergy;
	private int firstBar;
	
	private static final int WINDOW_SIZE = 2048;
	private static final Logger LOG = LoggerFactory.getLogger(MyBeatTimesProvider.class);
	private static final double WINDOW_STEP = 0.1;
	private static final int PLUS_MINUS_COUNT = 2; // how many values on the left and on the right will be used to smooth
	private static final double ONSET_TIME = 1.5; // max possible bar beginning delay
	
	public static final double SILENCE_SEC = 2.5;
	
	public static void main(String[] args) {
		//new MyBeatTimesProvider("D:/USU/MIR/SamplesWAV/BrainstormMaybe.wav");
		new MyBeatTimesProvider("D:/USU/MIR/MIREX '11/beattrack_train_2006/train/train14.wav");
	}

	public MyBeatTimesProvider(String waveFileName) {
		LOG.info("Detecting beats in " + waveFileName);
		double totalSeconds = 0;
		WavFile wavFile = null;
		
		// this will create a new FFT instance per each block of wave data
		// of length = windowSize samples
		ITransformProvider transformProvider = new ITransformProvider() {
			@Override
			public ITransform getTransform(Buffer buffer, CountDownLatch latch) {
				return new FFTTransformWrapper(buffer, latch);
			}
		};
		double[][] spectrum = null;
		// generate a time scale ranging from 0 to file length in seconds with step = WINDOW_STEP sec
		double[] windowBeginnings = BeatRootBeatTimesProvider.generateTimeScale(waveFileName, WINDOW_STEP);
		try {
			wavFile = WavFile.openWavFile(new File(waveFileName));
			
			// some values that can be useful, see other WavFile methods if needed
			samplingRate = (int) wavFile.getSampleRate();
			totalSeconds = wavFile.getNumFrames() * 1.0 / samplingRate;
			LOG.info(String.format("File length: %.2f s", totalSeconds));
			LOG.info(String.format("Sampling rate: %d samples per second", samplingRate));

			// the reader will provide wave data to transforms
			final WaveReader reader = new WaveReader(wavFile, windowBeginnings, WINDOW_SIZE);
			
			// this creates a multithreaded transformer which has 4 threads to run transforms
			final PooledTransformer transformer = new PooledTransformer(
					reader, 4, windowBeginnings.length, transformProvider);
			
			// here the actual processing is started
			spectrum = transformer.run();
			
			// here it is finished already
			LOG.info(String.format("There are %d spectrum columns, each of them contains %d values", spectrum.length, spectrum[0].length));
			
			// prepare labels for Y axis to draw spectrum
			String[] yText = new String[spectrum[0].length];
			for (int i = 0; i < yText.length; i++) {
				yText[i] = String.valueOf(samplingRate / 2 / spectrum[0].length * (i + 1));
			}
			Visualizer.visualizeSpectrum(spectrum, windowBeginnings, yText, "Spectrum");
			
			double[] energy = new double[spectrum.length];
			for (int i = 0; i < spectrum.length; i++) {
				double temp = 0;
				for (int j = 0; j < spectrum[i].length; j++) {
					// TODO: try to involve a volume level perception 
					temp += spectrum[i][j] * spectrum[i][j];
				}
				energy[i] = temp;
			}
			
			originalEnergy = energy;
			
			// draw a spectrum energy graph
			Visualizer.visualizeXByTimeDistribution(energy, windowBeginnings);
		
		} catch (Exception e) {
			LOG.error(String.format("Error when reading wave file %s", waveFileName), e);
		} finally {
			if (wavFile != null) { try {
				wavFile.close();
			} catch (IOException e) {
				LOG.error(String.format("Error when closing file %s", waveFileName), e);
			} }
		}
		
		// peaks above average value only
		double[] aboveAverage = getAboveAverageEnergy(originalEnergy);
		Visualizer.visualizeXByTimeDistribution(aboveAverage, windowBeginnings);
		
		// beginnings of bars
		double[] barBeginnings = calculateBarBeginnings(aboveAverage, SILENCE_SEC);	
		LOG.info(String.format("First bar is on %.2f s", firstBar * WINDOW_STEP) );
		//Visualizer.visualizeXByTimeDistribution(barBeginnings, windowBeginnings);
		
		double[] smoothEnergy = getSmoothEnergy(originalEnergy);
		Visualizer.visualizeXByTimeDistribution(smoothEnergy, windowBeginnings);
		
		int optimalTempo = getOptimalTempo(smoothEnergy);
		LOG.info(String.format("Optimal tempo is %.2f s", optimalTempo * WINDOW_STEP) );
		
		double[] kickLocations = getKicksLocations(originalEnergy.length, optimalTempo);
		Visualizer.visualizeXByTimeDistribution(kickLocations, windowBeginnings);
		
		String output = "D:/USU/MIR/MIREX '11/beattrack_train_2006/train/output.txt"; 
		// save beatTimes to output file
		BeatOnlyFileWriter.write(output, beatTimes);
	}

	private double[] getSmoothEnergy(double[] original) {
		int len = original.length;
		double[] smoothed = new double[len];
		for (int i=0; i<len; ++i) {
			int left = Math.max(0, i - PLUS_MINUS_COUNT);
			int right = Math.min(len, i + PLUS_MINUS_COUNT);
			//double sum = 0;
			for (int j=left; j<right; ++j) {
				//sum += original[j];
				if (original[j] > smoothed[i])
					//sum = original[j];
					smoothed[i] = original[j];
			}
			//smoothed[i] = sum / (right - left); 
		}
		return smoothed;
	}

	// calculates beatTimes
	// and returns array where 0 if no beat, > 0 - otherwise. 
	private double[] getKicksLocations(int length, int optimalPeriod) {
		double[] result = new double[length];
		int rest = firstBar % optimalPeriod;
		int countOfBeats = (length - rest) / optimalPeriod + 1;
		beatTimes = new double[countOfBeats];
		for (int i=rest, k=0; i<length; i+=optimalPeriod, ++k) {
			result[i] = 100000.0;
			beatTimes[k] = i / WINDOW_STEP / 1000; // divided by 1000 because ms -> sec
		}
		return result;
	}
	
	private int getOptimalTempo(double[] smoothed) {
		double minTempoDuration = 0.25;
		double maxTempoDuration = 3;
		int minSamples = (int) Math.floor(minTempoDuration / WINDOW_STEP);
		int maxSamples = (int) Math.floor(maxTempoDuration / WINDOW_STEP);
		double[] tempo = new double[maxSamples];
		int optimalTempoDuration = minSamples;
		for (int period=minSamples; period<maxSamples; ++period) {
			int rest = firstBar % period; 
			int kicksCount = (smoothed.length - rest) / period;
			for (int j=rest; j<smoothed.length; j+=period)
				tempo[period] += smoothed[j] / kicksCount; 
			
			// TODO: try to match all bar beginnings with tempo grid
			
			// apply perception model (normal distribution for tempo) 
			double currentTempo = 1.0 * period / WINDOW_STEP; // current tempo duration in sec
			int bpm = (int) (60.0 / currentTempo); // beats per minute
			
			double mean = 120;
			double sigma = 55;
			double prob = Math.exp(-(bpm - mean) * (bpm - mean) / (2*sigma*sigma)) / Math.sqrt(2*Math.PI*sigma*sigma);
			tempo[period] *= prob;
			
			if (tempo[period] > tempo[optimalTempoDuration])
				optimalTempoDuration = period;
		}
		return optimalTempoDuration;
	}

	// value will be 1 if this timestamp is bar beginning, 0 otherwise 
	private double[] calculateBarBeginnings(double[] original, double silenceSec) {
		// firstly long silence (more than silenceSec), then beginning of bar
		// count of zero energy samples followed by bar beginning
		int maxSamplingsCount = (int) Math.floor(silenceSec / WINDOW_STEP);
		// count of continuous zeros in array
		int zeroCount = 0; 
		int maxEnergyLocation = 0;
		double[] barBeginnings = new double[original.length];
		firstBar = -1;
		for (int i=0; i<original.length; ++i) {
			if (original[i] == 0) {
				++zeroCount;
			} else {
				if (zeroCount > maxSamplingsCount) {
					// find the nearest local maximum within ONSET_TIME period of time
					int min = Math.min(i + (int) Math.floor(ONSET_TIME / WINDOW_STEP), original.length);
					int localMaxLocation = i;
					for (int j=i+1; j<min; ++j) {
						if (original[j] > original[localMaxLocation])
							localMaxLocation = j;
					}	
					barBeginnings[localMaxLocation] = 100000.0;
					double time = (double) localMaxLocation / samplingRate;
					LOG.info(String.format("beginning of the next bar - %.2f s", time) );
					
					// if value is not assigned yet
					if (firstBar == -1)							
						firstBar = localMaxLocation;
					
					i = localMaxLocation;
				} else {
					if (original[i] > original[maxEnergyLocation])
						maxEnergyLocation = i;
				}
				zeroCount = 0;
			}
		}
		// if value is not assigned yet
		if (firstBar == -1)
			firstBar = maxEnergyLocation;
		
		return barBeginnings;
	}

	private double[] getAboveAverageEnergy(double[] original) {
		double[] result = original.clone();
		
		double avgEnergy = 0;
		for (int i=0; i<original.length; ++i) {
			avgEnergy += result[i] / original.length;
		}
		avgEnergy *= 1.5; // magic const
		for (int i=0; i<original.length; ++i) {
			result[i] = Math.max(0, result[i] - avgEnergy);
		}
		return result;
	}

	@Override
	public double[] getBeatTimes() {
		return beatTimes;
	}

}
