package chordest.transform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import chordest.transform.PooledTransformer.ITransformProvider;
import chordest.wave.Buffer;
import chordest.wave.ITaskProvider;
import chordest.wave.ReadOnlyBuffer;


public class DiscreteCosineTransform {

	private static double[] transform(double[] data) {
		int size = data.length;
		double[] result = new double[size];
		double PS = Math.PI / size;
		for (int i = 0; i < size; i++) {
			double PSI = PS * i;
			double sum = 0;
			for (int j = 0; j < size; j++) {
				sum += (Math.cos(PSI * (j + 0.5)) * data[j]);
			}
			result[i] = sum;
		}
		return result;
	}

	private static double[] inverse(double[] data) {
		int size = data.length;
		double[] result = new double[size];
		double PS = Math.PI / size;
		for (int i = 0; i < size; i++) {
			double PSI2 = PS * (i + 0.5);
			double sum = data[0] * 0.5;
			for (int j = 0; j < size; j++) {
				sum += (data[j] * Math.cos(PSI2 * j));
			}
			result[i] = sum;
		}
		return result;
	}

	private static double[] doChromaReduction(double[] data, int firstNonZero) {
		double[] dct = transform(data);
		firstNonZero = Math.min(firstNonZero, dct.length);
		for (int i = 0; i < firstNonZero; i++) {
			dct[i] = 0;
		}
		return inverse(dct);
	}

	public static double[][] doChromaReduction(final double[][] data, final int firstNonZero) {
		ITaskProvider taskProvider = new ITaskProvider() {
			private AtomicInteger idx = new AtomicInteger(0);
			@Override
			public void run() {
				// TODO Auto-generated method stub
			}

			@Override
			public Buffer poll() throws InterruptedException {
				int currentIdx = idx.getAndAdd(1);
				if (currentIdx >= data.length) {
					return null;
				}
				Buffer buffer = new Buffer(data[currentIdx].length, currentIdx);
				buffer.append(data[currentIdx]);
				return buffer;
			} };
		ITransformProvider transformProvider = new ITransformProvider() {
			@Override
			public ITransform getTransform(final Buffer buffer, final CountDownLatch latch) {
				return new ITransform() {
					@Override
					public Buffer call() throws Exception {
						try {
							double[] result = doChromaReduction(buffer.getData(), firstNonZero);
							return new ReadOnlyBuffer(result, buffer.getTimeStamp());
						} finally {
							latch.countDown();
						}
					}};
			} };
		final PooledTransformer transformer = new PooledTransformer(
				taskProvider, 8, data.length, transformProvider);
		double[][] result;
		try {
			result = transformer.run();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return result;
	}

}
