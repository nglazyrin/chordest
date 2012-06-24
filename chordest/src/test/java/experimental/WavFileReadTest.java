package experimental;
import java.io.File;
import java.io.IOException;

import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.gui.JFreeChartUtils;
import chordest.util.DatasetUtil;

import uk.co.labbookpages.WavFile;


public class WavFileReadTest {

	private static final String FILENAME = "../samples/sheep.wav";
	private static final Logger LOG = LoggerFactory.getLogger(WavFileReadTest.class);

	public static void main(String[] args) {
		WavFile wavFile = null;
		try {
			wavFile = WavFile.openWavFile(new File(FILENAME));
			int samplingRate = (int) wavFile.getSampleRate();
			int channels = wavFile.getNumChannels();
			int frames = (int) wavFile.getNumFrames();
			double seconds = frames * 1.0 / samplingRate;
			double step = seconds / frames;
			double[] xLabels = new double[frames];
			for (int i = 0; i < frames; i++) {
				xLabels[i] = i * step;
			}
			int[][] data = new int[channels][];
			for (int i = 0; i < channels; i++) {
				data[i] = new int[frames];
			}
			int framesRead = wavFile.readFrames(data, frames);
			LOG.info("Frames read: " + framesRead);
			XYDataset channel0 = DatasetUtil.toXYDataset(xLabels, data[0]);
			JFreeChartUtils.visualize("Channel 0", "Time", "Amplitude", channel0);
			XYDataset channel1 = DatasetUtil.toXYDataset(xLabels, data[1]);
			JFreeChartUtils.visualize("Channel 1", "Time", "Amplitude", channel1);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (wavFile != null) { try {
				wavFile.close();
			} catch (IOException ignore) {} }
		}
	}

}
