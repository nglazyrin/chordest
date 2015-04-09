package chordest.beat;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.transform.FFTTransformWrapper;
import chordest.transform.ITransform;
import chordest.transform.PooledTransformer;
import chordest.transform.PooledTransformer.ITransformProvider;
import chordest.util.DataUtil;
import chordest.util.Visualizer;
import chordest.wave.Buffer;
import chordest.wave.WaveFileInfo;
import chordest.wave.WaveReader;

public class MyBeatTimesProvider implements IBeatTimesProvider {

	private static final Logger LOG = LoggerFactory.getLogger(MyBeatTimesProvider.class);

	public static void main(String[] args) {
		new MyBeatTimesProvider("E:/Dev/workspace/samples/august.wav");
	}

	public MyBeatTimesProvider(String waveFileName) {
		LOG.info("Detecting beats in " + waveFileName);
		int windowSize = 2048;
		
		// this will create a new FFT instance per each block of wave data
		// of length = windowSize samples
		ITransformProvider transformProvider = new ITransformProvider() {
			@Override
			public ITransform getTransform(Buffer buffer, CountDownLatch latch) {
				return new FFTTransformWrapper(buffer, latch);
			}
		};
		double[][] spectrum = null;
		// generate a time scale ranging from 0 to file length in seconds with step = 0.1 s
		double[] windowBeginnings = DataUtil.generateDefaultBeats(waveFileName, 0.5);
		try {
			WaveFileInfo wfi = new WaveFileInfo(waveFileName);
			if (wfi.exception != null) {
				throw wfi.exception;
			}
			// some values that can be useful
			LOG.info(String.format("File length: %.2f s", wfi.seconds));
			LOG.info(String.format("Sampling rate: %d samples per second", wfi.samplingRate));

			// the reader will provide wave data to transforms
			final WaveReader reader = new WaveReader(new File(waveFileName), windowBeginnings, windowSize);
			
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
				yText[i] = String.valueOf(wfi.samplingRate / 2 / spectrum[0].length * (i + 1));
			}
			Visualizer.visualizeSpectrum(spectrum, windowBeginnings, yText, "Spectrum");
			
			double[] energy = new double[spectrum.length];
			for (int i = 0; i < spectrum.length; i++) {
				double temp = 0;
				for (int j = 0; j < spectrum[i].length; j++) {
					temp += spectrum[i][j] * spectrum[i][j];
				}
				energy[i] = temp;
			}
			// draw a spectrum energy graph
			Visualizer.visualizeXByTimeDistribution(energy, windowBeginnings);
		} catch (Exception e) {
			LOG.error(String.format("Error when reading wave file %s", waveFileName), e);
		}
		LOG.info(String.format("Finished processing of %s", waveFileName));
	}

	@Override
	public double[] getBeatTimes() {
		// TODO Auto-generated method stub
		return null;
	}

}
