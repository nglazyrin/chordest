package chordest.wave;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.util.DataUtil;

import uk.co.labbookpages.WavFile;

public class WaveReader implements ITaskProvider {

	private static final Logger LOG = LoggerFactory.getLogger(WaveReader.class);
	private static final int QUEUE_SIZE = 100;
	
	private final double[] times;
	private final int[] offsets;
	
	private final WavFile wavFile;
	private final int samplingRate;
	private final int channels;
	private final int frames;
	private final double seconds;

	private final int bufferSize;
	private final BlockingQueue<Buffer> buffers;

	/**
	 * Reads the WavFile by placing frames at specified time moments
	 * @param wavFile Should be just opened, but not yet read
	 * @param times
	 */
	public WaveReader(WavFile wavFile, double[] times, int bufferSize) {
		if (wavFile == null) {
			throw new NullPointerException("wavFile should not be null");
		}
		if (times == null) {
			throw new NullPointerException("times should not be null");
		}
		this.wavFile = wavFile;
		this.times = times;
		
		this.bufferSize = bufferSize;
		
		this.samplingRate = (int) wavFile.getSampleRate();
		this.channels = wavFile.getNumChannels();
		this.frames = (int) wavFile.getNumFrames();
		this.seconds = this.frames * 1.0 / this.samplingRate;
		
		this.buffers = new ArrayBlockingQueue<Buffer>(QUEUE_SIZE, true); //TODO: calculate automatically

		this.offsets = new int[times.length + 1]; // one element bigger than times
		double framesPerSecond = frames / seconds;
		for (int i = 0; i < times.length; i++) {
			offsets[i] = (int) (times[i] * framesPerSecond);
		}
		offsets[times.length] = frames;
	}

	public void read() throws IOException, InterruptedException {
		skipDataBeforeFirstTimeStamp();
		
		for (int i = 0; i < times.length; i++) {
			// number of frames to be read at this iteration
			final int currentSize = offsets[i+1] - offsets[i];
			
			// create and fill temporary buffer
			double[][] current = createArray(channels, currentSize);
			wavFile.readFrames(current, 0, currentSize);
			double[] data = current[0];
			for (int j = 1; j < channels; j++) {
				data = DataUtil.add(data, current[j]);
			}
			data = DataUtil.multiply(data, 1.0 / channels);
			current = null;
			
			// create new buffer for this timestamp
			// use put instead of add because it is a blocking queue
			buffers.put(new Buffer(bufferSize, times[i]));
			
			// append the data from temporary buffer to all buffers
			for (Buffer buffer : buffers) {
				if (! buffer.isFull()) {
					buffer.append(data);
				}
			}
		}
		// close all remaining buffers
		for (Buffer buffer : buffers) {
			buffer.close();
		}
		LOG.debug("... finished reading wave data ...");
	}

	private void skipDataBeforeFirstTimeStamp() throws IOException {
		if (times != null && times.length > 0 && times[0] > 0) {
			final int firstOffset = offsets[0];
			final double[][] ignore = createArray(channels, firstOffset);
			wavFile.readFrames(ignore, firstOffset);
		}
	}

	private double[][] createArray(int d1, int d2) {
		final double[][] result = new double[d1][];
		if (d2 < 0) { d2 = 0; }
		for (int i = 0; i < d1; i++) {
			result[i] = new double[d2];
		}
		return result;
	}

	@Override
	public Buffer poll() throws InterruptedException {
		if (buffers.size() > 0 && buffers.peek().isFull()) {
			// use take() instead of poll() because it is a blocking queue
			return buffers.take();
		}
		return null;
	}

	@Override
	public void run() {
		try {
			read();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				wavFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
