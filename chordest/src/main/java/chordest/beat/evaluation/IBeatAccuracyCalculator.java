package chordest.beat.evaluation;

public interface IBeatAccuracyCalculator {
	// compare two given arrays and calculate similarity measure percent
	double getBeatAccuracy(double[] actual, double[] expected);
}
