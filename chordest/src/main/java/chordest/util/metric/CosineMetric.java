package chordest.util.metric;

public class CosineMetric implements IMetric {

	@Override
	public double distance(double[] from, double[] to) {
		if (from.length != to.length) {
			throw new RuntimeException("Vector have different lengths");
		}
		double sum = 0;
		int len = from.length;
		for (int i = 0; i < len; i++) {
			sum += (from[i] * to[i]);
		}
		sum /= norm(from);
		sum /= norm(to);
		return 1 - sum;
	}

	@Override
	public double[] normalize(double[] vector) {
		double[] result = new double[vector.length];
		double norm = norm(vector);
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i] / norm;
		}
		return result;
	}

	private double norm(double[] vector) {
		double sum = 0;
		int len = vector.length;
		for (int i = 0; i < len; i++) {
			sum += vector[i] * vector[i];
		}
		return Math.sqrt(sum);
	}

}
