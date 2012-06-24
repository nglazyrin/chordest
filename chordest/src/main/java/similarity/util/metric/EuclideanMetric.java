package similarity.util.metric;

public class EuclideanMetric implements IMetric {

	@Override
	public double distance(double[] from, double[] to) {
		if (from.length != to.length) {
			throw new RuntimeException("Vector have different lengths");
		}
		double sum = 0;
		for (int i = 0; i < from.length; i++) {
			sum = sum + (from[i] - to[i])*(from[i] - to[i]);
		}
		return Math.sqrt(sum);
	}

	@Override
	public double[] normalize(double[] vector) {
		double sum = 0;
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			sum += vector[i] * vector[i];
		}
		sum = Math.sqrt(sum);
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i] / sum;
		}
		return result;
	}

}
