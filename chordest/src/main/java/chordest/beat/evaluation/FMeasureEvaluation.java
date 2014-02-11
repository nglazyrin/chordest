package chordest.beat.evaluation;

public class FMeasureEvaluation implements IBeatAccuracyCalculator {

	// kick would be correct within this period of time in milliseconds (+/- from expected kick)
	private final double delta = 70;
	
	private int correctCount;
	private int missedCount;
	private int extraCount;
	
	@Override
	public double getBeatAccuracy(double[] actual, double[] expected) {
		calculateAllValues(actual, expected);
		return 2.0 * correctCount * 100 / (2*correctCount + extraCount + missedCount);
	}
	
	private void calculateAllValues(double[] actual, double[] expected) {
		int k = 0; // current position of element in expected array
		for (int i=0; i<actual.length; ++i) {
			// find left bound of possible expected kick near the actual kick 
			while (k < expected.length && expected[k] < actual[i] - delta) {
				++k;
				++missedCount;
			}
			if (k >= expected.length) {
				extraCount += actual.length - i;
				break;
			}
			else {
				if (expected[k] > actual[i] + delta)
					++extraCount;
				else {
					++correctCount;
					++k;
				}
			}
		}
		
		if (k < expected.length)
			missedCount += expected.length - k;
	}
}
