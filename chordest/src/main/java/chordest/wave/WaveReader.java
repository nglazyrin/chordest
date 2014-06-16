package chordest.wave;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.util.DataUtil;

public class WaveReader implements ITaskProvider {

	private static final Logger LOG = LoggerFactory.getLogger(WaveReader.class);
	private static final int QUEUE_SIZE = 100;
	
	private final double[] times;
	private final long[] offsets;
	
	private final AudioInputStream stream;
	private final AudioFormat format;
	
	private final int samplingRate;
	private final int channels;
	private final int frames;
	private final double seconds;

	private final int bufferSize;
	private final BlockingQueue<Buffer> buffers;

	private final Object lock = new Object();

	/**
	 * Reads the file by placing frames at specified time moments
	 * @param file
	 * @param times
	 * @param bufferSize 
	 */
	public WaveReader(File file, double[] times, int bufferSize) {
		if (file == null) {
			throw new NullPointerException("file should not be null");
		}
		if (times == null) {
			throw new NullPointerException("times should not be null");
		}
		this.times = times;
		this.bufferSize = bufferSize;
		
		try {
			this.stream = AudioSystem.getAudioInputStream(file);
		} catch (UnsupportedAudioFileException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		this.format = stream.getFormat();
		if (format.getSampleSizeInBits() % 8 != 0 || format.getSampleSizeInBits() > 16) {
			throw new RuntimeException("Wave files with " + format.getSampleSizeInBits() + "-bit sample size are not supported");
		}
		Encoding encoding = format.getEncoding();
		if (! (Encoding.PCM_SIGNED.equals(encoding) || Encoding.PCM_UNSIGNED.equals(encoding))) {
			throw new RuntimeException("Encoding " + encoding.toString() + " is not supported");
		}
		
		this.channels = format.getChannels();
		this.samplingRate = (int) format.getSampleRate();
		this.frames = (int) stream.getFrameLength();
		this.seconds = this.frames * 1.0 / this.samplingRate;
		
		this.buffers = new ArrayBlockingQueue<Buffer>(QUEUE_SIZE, true); //TODO: calculate automatically

		this.offsets = new long[times.length + 1]; // one element bigger than times
		double framesPerSecond = frames / seconds;
		for (int i = 0; i < times.length; i++) {
			offsets[i] = (long) (times[i] * framesPerSecond);
		}
		offsets[times.length] = frames;
	}

	public void read() throws IOException, InterruptedException {
		skipDataBeforeFirstTimeStamp();
		
		for (int i = 0; i < times.length; i++) {
			// number of frames to be read at this iteration
			final int currentSize = (int) (offsets[i+1] - offsets[i]);
			
			// create and fill temporary buffer
			double[][] current;
			if (offsets[i] >= 0) {
				current = readData(currentSize);
			} else {
				current = createArray(channels, currentSize);
			}
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
			synchronized (lock) {
				for (Buffer buffer : buffers) {
					if (! buffer.isFull()) {
						buffer.append(data);
					}
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
			final int firstOffset = (int) offsets[0];
//			final double[][] ignore = createArray(channels, firstOffset);
//			wavFile.readFrames(ignore, firstOffset);
			readData(firstOffset);
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
		synchronized (lock) {
			if (buffers.size() > 0 && buffers.peek().isFull()) {
				// use take() instead of poll() because it is a blocking queue
				return buffers.take();
			}
			return null;
		}
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
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private double[][] readData(int size) {
		// http://blog.bjornroche.com/2013/05/the-abcs-of-pcm-uncompressed-digital.html
		// frame consists of one sample per each channel
		// see also
		//http://www.sonicspot.com/guide/wavefiles.html
		//http://www.blitter.com/~russtopia/MIDI/~jglatt/tech/wave.htm
		double[][] result = createArray(channels, size);
		int bytesPerFrame = format.getFrameSize();
		if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
			bytesPerFrame = 1;
		}
		int bitsPerSample = format.getSampleSizeInBits();
		int numBytes = size * bytesPerFrame;
		double scale = 0;
		double bias = 0;
		if (bitsPerSample > 8) {
			// signed data
			scale = Long.MAX_VALUE >> (64 - bitsPerSample);
			bias = 0;
		} else {
			// unsigned data
			scale = 1 / (0.5 * ((1 << bitsPerSample) - 1));
			bias = -1;
		}
		byte[] audioBytes = new byte[numBytes];
		try {
		    int bytesRead = stream.read(audioBytes, 0, numBytes);
	    	int framesRead = bytesRead / bytesPerFrame;
	    	for (int frame = 0; frame < framesRead; frame++) {
	    		int frameStart = frame * bytesPerFrame;
	    		for (int channel = 0; channel < channels; channel++) {
	    			switch (bitsPerSample) {
	    			case 8:
	    				result[channel][frame] = bias + audioBytes[channel + frameStart] * scale;
	    				break;
	    			case 16:
	    				int b0 = audioBytes[2 * channel + frameStart];
	    				int b1 = audioBytes[2 * channel + frameStart + 1];
	    				if (format.isBigEndian()) {
	    					result[channel][frame] = bias + ((b0 << 8) + b1) * scale;
	    				} else {
	    					result[channel][frame] = bias + ((b1 << 8) + b0) * scale;
	    				}
	    				break;
	    			}
	    		}
	    	}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    	return result;
	}

}
