package chordest.transform;

public class DiscreteCosineTransform {

	public static double[] transform(double[] data) {
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

	public static double[] inverse(double[] data) {
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

	public static double[] doChromaReduction(double[] data) {
		int N = 15;
		double[] dct = transform(data);
		for (int i = 0; i < N; i++) {
			dct[i] = 0;
		}
		return inverse(dct);
	}

	public static double[][] doChromaReduction(double[][] data) {
		double[][] result = new double[data.length][];
		for (int i = 0; i < data.length; i++) {
			result[i] = doChromaReduction(data[i]);
		}
		return result;
	}

}
