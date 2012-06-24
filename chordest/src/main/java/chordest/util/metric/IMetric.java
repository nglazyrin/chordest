package chordest.util.metric;

public interface IMetric {

	public double distance(double[] from, double[] to);
	public double[] normalize(double[] array);
}
