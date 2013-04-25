package chordest.util.metric;

public class KLMetric implements IMetric {

	@Override
	public double distance(double[] from, double[] to) {
		if (from.length != to.length) {
			throw new RuntimeException("Vector have different lengths");
		}
		double sum = 0;
		for (int i = 0; i < from.length; i++) {
			double u = from[i] + 1;
			double v = to[i] + 1;
			sum = sum + (u * Math.log(u / v)); // - u + v);
		}
		return sum;
	}

	@Override
	public double[] normalize(double[] vector) {
		double sum = 0;
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			sum += vector[i] * vector[i];
		}
		sum = Math.sqrt(sum);
		if (sum > 0) {
			for (int i = 0; i < vector.length; i++) {
				result[i] = vector[i] / sum;
			}
			return result;
		} else {
			return vector;
		}
	}

}
