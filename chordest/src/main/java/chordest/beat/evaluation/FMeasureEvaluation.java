package chordest.beat.evaluation;

import java.util.HashMap;
import java.util.Map;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.MyBeatTimesProvider;

public class FMeasureEvaluation implements IBeatAccuracyCalculator {

	private static final Logger LOG = LoggerFactory.getLogger(MyBeatTimesProvider.class);
	
	// kick would be correct within this period of time in seconds (+/- from expected kick)
	private final double delta = 0.08;
	
	private int correctCount;
	private int missedCount;
	private int extraCount;
	
	@Override
	public double getBeatAccuracy(double[] actual, double[] expected) {
		ClearAllValues();
		
		// alternate way to combine all 40 ground-truth sequences from musicians 
		// and compare calculated array with single but combined from all tapped ones.
		// calculateAllValuesFull(actual, expected);
		
		calculateAllValues(actual, expected);
		
		double result = 2.0 * correctCount * 100 / (2*correctCount + extraCount + missedCount);
		
		LOG.info("c=" + correctCount + " m=" + missedCount + " e=" + extraCount);
		
		return result;
	}
	
	private void ClearAllValues() {
		correctCount = 0;
		missedCount = 0;
		extraCount = 0;
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
	
	private void calculateAllValuesFull(double[] actual, double[] expected) {
		// expected - sorted points from all beats sequences
		
		// count of people at least who must point our value as beat time within delta period 
		final int lowCount = 10;
		
		// count of people at least who must point not our value as beat time
		final int highCount = 30;
		
		boolean[] exclude = new boolean[expected.length];
		
		for (int i=0; i<actual.length; ++i) {
			double left = actual[i] - 2 * delta;
			double right = actual[i] + 2 * delta;
			
			if (InInterval(expected, left, right, lowCount, exclude, true))
				++correctCount;
			else
				++extraCount;
		}
		
		for (int i=0; i<expected.length; ++i) {
			// if i point is in interval of correct point
			if (exclude[i])
				continue;
			
			double left = expected[i];
			double right = left + delta;
			
			if (InInterval(expected, left, right, highCount, exclude, false)) {
				++missedCount;
				
				while (i < expected.length && expected[i] < right)
					++i;
			}
		}
	}

	private boolean InInterval(double[] expected, double left, double right, int count, 
			boolean[] exclude, boolean needToExclude) {
		int k = 0;
		while (k < expected.length && expected[k] < left)
			++k;
		
		if (k >= expected.length)
			return false;
		
		int left_k = k;
		
		int kol = 0;
		
		while (k < expected.length && expected[k] < right) {
			if (!exclude[k]) {
				++kol;
			}
			++k;
		}
		
		if (kol >= count) {
			if (needToExclude) {
				for (int i=left_k; i<k; ++i) {
					exclude[i] = true;
				}
			}
			return true;
		}
		
		return false;
	}
}
