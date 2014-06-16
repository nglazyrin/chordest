package chordest.beat.evaluation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import chordest.beat.MyBeatTimesProvider;

public class CemgilEvaluation implements IBeatAccuracyCalculator {

	// variance in normal distribution (in milliseconds)
	private final double sigma = 40;
	
	@Override
	public double getBeatAccuracy(double[] actual, double[] expected) {
		// uncomment this to adjust sequence of beats when all 40 ground-truth sequences are combined in only one
		//expected = AdjustExpectedBeatsForCemgil(expected);
		
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
			minDistance *= 1000; // seconds to milliseconds
			
			// take into account the inaccuracy of calculation
			double correctDist = minDistance;
			
			// if distance within 10 ms then it is just inaccuracy and should be zero
			if (correctDist < MyBeatTimesProvider.WINDOW_STEP * 1000)
				correctDist = 0;
			else
				correctDist -= MyBeatTimesProvider.WINDOW_STEP * 1000;
			
			// apply normal distribution
			result += Math.exp(-correctDist*correctDist/(2*sigma*sigma));
		}
		
		return result * 100 * 2 / (actual.length + expected.length);
	}
	
	// used for manipulating of expected sequence for comparing with single 
	// but combined sequence of beats from 40 musicians
	private double[] AdjustExpectedBeatsForCemgil(double[] expectedBeats) {
		int minHits = 30;
		double delta = 0.040;
		List<Double> adjusted = new LinkedList<Double>();
		
		for (int i = 0; i < expectedBeats.length; i++) {
			double left = expectedBeats[i];
			double right = expectedBeats[i] + 2 * delta;
			
			int j = i;
			int count = 0;
			while (j < expectedBeats.length && expectedBeats[j] < right) {
				count++;
				j++;
			}
			
			if (count >= minHits) {
				adjusted.add((left + right)/2);
				i = j;
			}
		}
		
		double[] result = new double[adjusted.size()];
		for (int i = 0; i < adjusted.size(); i++) {
			result[i] = (double) adjusted.get(i);
		}

		return result;
	}
}
