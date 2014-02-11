package chordest.beat.evaluation;

public class CemgilEvaluation implements IBeatAccuracyCalculator {

	// variance in normal distribution (in milliseconds)
	private final double sigma = 40;
	
	@Override
	public double getBeatAccuracy(double[] actual, double[] expected) {
		if (actual.length == 0)
			return expected.length == 0 ? 100 : 0;
		
		double result = 0;
		
		int k = 0; // current actual element
		for (int i=0; i<expected.length; ++i) {
			double minDistance = Math.abs(expected[i] - actual[k]);
			++k;
			while (k < actual.length && Math.abs(expected[i] - actual[k]) < minDistance) {
				minDistance = Math.abs(expected[i] - actual[k]);
				++k;
			}
			// step back anyway
			--k;
			minDistance *= 1000; // milliseconds to seconds
			// apply normal distribution
			result += Math.exp(-minDistance*minDistance/(2*sigma*sigma));
		}
		
		return result * 100 * 2 / (actual.length + expected.length);
	}
}
