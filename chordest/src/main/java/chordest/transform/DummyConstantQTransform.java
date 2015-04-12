package chordest.transform;

import java.util.concurrent.CountDownLatch;

import chordest.wave.Buffer;
import chordest.wave.ReadOnlyBuffer;


public class DummyConstantQTransform extends AbstractTransform {

	private CQConstants cqConstants;
	private ScaleInfo scaleInfo;
	private Buffer buffer;

	public static double[] transform(double[] data, ScaleInfo scaleInfo) {
		Buffer buffer = new ReadOnlyBuffer(data, 0);
		DummyConstantQTransform tr = new DummyConstantQTransform(buffer, scaleInfo, null);
		return tr.transform().getData();
	}

	public DummyConstantQTransform(Buffer buffer, ScaleInfo scaleHolder, CountDownLatch latch) {
		this(buffer, scaleHolder, latch, CQConstants.getDefaultInstance());
	}

	public DummyConstantQTransform(Buffer buffer, ScaleInfo scaleHolder,
			CountDownLatch latch, CQConstants consts) {
		super(latch);
		this.buffer = buffer;
		this.scaleInfo = scaleHolder;
		cqConstants = consts;
	}

	@Override
	Buffer transform() {
//		int maxWindowLength = cqConstants.getLongestWindow();
		double[] data = buffer.getData();
//		if (data.length >= maxWindowLength) {
			int spectrumSize = scaleInfo.getTotalComponentsCount();
			double[] spectrum = new double[spectrumSize];
			for (int kcq = 0; kcq < spectrumSize; kcq++) {
				int Nkcq = cqConstants.componentWindowLengths[kcq];
				final double[] windowFunction = cqConstants.windowFunctions[kcq];
				double result_re = 0;
				double result_im = 0;
				int startIndex = (data.length - Nkcq)/2;
				for (int n = 0; n < Nkcq; n++) {
					double m = data[n + startIndex] * windowFunction[n];
					double sin = cqConstants.sinuses[kcq][n];
					double cos = cqConstants.cosinuses[kcq][n];
					result_re += (cos*m);
					result_im += (sin*m);
				}
				spectrum[kcq] = Math.sqrt(result_re * result_re + result_im * result_im) / Nkcq ;
			}
			double timeStamp = buffer.getTimeStamp();
			buffer = null; // free the memory (~400 kB per each buffer, it is important)
			return new ReadOnlyBuffer(spectrum, timeStamp);
//		} else {
//			throw new RuntimeException(String.format(
//					"Cannot perform transform: max window length '%d' is greater than data length '%d'", 
//					maxWindowLength, data.length));
//		}
	}

}
