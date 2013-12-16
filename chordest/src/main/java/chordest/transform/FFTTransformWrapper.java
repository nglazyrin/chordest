package chordest.transform;

import java.util.concurrent.CountDownLatch;

import chordest.wave.Buffer;
import chordest.wave.ReadOnlyBuffer;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FFTTransformWrapper extends AbstractTransform {

	// TODO: may not work properly!
	
	private Buffer buffer;

	private CQConstants cqConstants;

	public FFTTransformWrapper(Buffer buffer, CountDownLatch latch) {
		super(latch);
		this.buffer = buffer;
	}

	public FFTTransformWrapper(Buffer buffer, CountDownLatch latch, CQConstants cqc) {
		this(buffer, latch);
		this.cqConstants = cqc;
	}

	@Override
	Buffer transform() {
		double[] data = buffer.getData();
		DoubleFFT_1D fft = new DoubleFFT_1D(data.length);
		fft.realForward(data);
		double timeStamp = buffer.getTimeStamp();
		double[] spectrum = new double[data.length / 2];
//		spectrum[0] = data[0];
		spectrum[spectrum.length - 1] = data[1];
		for (int i = 1; i < data.length / 2; i++) {
			spectrum[i-1] = Math.sqrt(data[2*i] * data[2*i] + data[2*i + 1] * data[2*i + 1]);
		}
		buffer = null;
		if (cqConstants != null) {
			spectrum = toLogSpacedSpectrum(spectrum);
		}
		return new ReadOnlyBuffer(spectrum, timeStamp);
	}

	/**
	 * минимальная частота = частота дискретизации / длина окна в сэмплах.
	 * Последующие частоты кратны минимальной.
	 * @param spectrum
	 * @return
	 */
	private double[] toLogSpacedSpectrum(double[] spectrum) {
		double[] frequencies = cqConstants.componentFrequencies;
		int size = frequencies.length;
		double[] result = new double[size];
		double minFreq = cqConstants.getSamplingRate() * 1.0 / spectrum.length;
		int[] counts = new int[size];
		for (int iFFT = 0; iFFT < spectrum.length; iFFT++) {
			double freq = minFreq * (iFFT + 1);
			int iCQT = 0;
			while (iCQT < size && frequencies[iCQT] < freq) {
				iCQT++;
			}
			if (iCQT > 0 && iCQT < size) {
				if (Math.abs(freq - frequencies[iCQT - 1]) > Math.abs(frequencies[iCQT] - freq)) {
				} else {
					iCQT--;
				}
				result[iCQT] += spectrum[iFFT];
				counts[iCQT]++;
			}
		}
		for (int i = 0; i < size; i++) {
			if (counts[i] > 0) {
				result[i] /= counts[i];
			}
		}
		return result;
	}

}
