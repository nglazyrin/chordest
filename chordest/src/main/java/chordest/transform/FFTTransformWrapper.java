package chordest.transform;

import java.util.concurrent.CountDownLatch;

import chordest.wave.Buffer;
import chordest.wave.ReadOnlyBuffer;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FFTTransformWrapper extends AbstractTransform {

	// TODO: may not work properly!
	
	private Buffer buffer;

	private final CQConstants cqConstants;

	public FFTTransformWrapper(Buffer buffer, CountDownLatch latch, CQConstants cqc) {
		super(latch);
		this.buffer = buffer;
		this.cqConstants = cqc;
	}

	@Override
	Buffer transform() {
		double[] data = buffer.getData();
		DoubleFFT_1D fft = new DoubleFFT_1D(data.length);
		fft.realForward(data);
		double timeStamp = buffer.getTimeStamp();
		double[] spectrum = new double[data.length / 2];
		spectrum[spectrum.length - 1] = data[1];
		for (int i = 1; i < data.length / 2; i++) {
			spectrum[i - 1] = Math.sqrt(data[2*i] * data[2*i] + data[2*i + 1] * data[2*i + 1]);
		}
		buffer = null;
		spectrum = toLogSpacedSpectrum(spectrum);
		return new ReadOnlyBuffer(spectrum, timeStamp);
	}

	private double[] toLogSpacedSpectrum(double[] spectrum) {
		double[] frequencies = cqConstants.getComponentFrequencies();
		int size = frequencies.length;
		double[] result = new double[size];
		for (int k = 0; k < spectrum.length; k++) {
			int p = (int) (Math.round(size * Math.log(k / spectrum.length * 44100 / 440) / Math.log(2))) % size;
			result[p] += spectrum[k];
		}
		return result;
	}

}
