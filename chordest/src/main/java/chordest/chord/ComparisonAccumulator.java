package chordest.chord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import chordest.util.MapUtil;

public class ComparisonAccumulator {

	private double totalOverlap = 0;
	private double totalWeightedOverlap = 0;
	private double totalSegmentation = 0;
	private int totalTracks = 0;
	private double totalEffectiveLength = 0;
	private final Map<String, Double> errors = new HashMap<String, Double>();

	public void append(ChordListsComparison cmp) {
		final double overlap = cmp.getOverlapMeasure();
		final double effectiveSeconds = cmp.getEffectiveSeconds();
		totalOverlap += overlap;
		totalWeightedOverlap += (overlap * effectiveSeconds);
		totalSegmentation += cmp.getSegmentation();
		totalEffectiveLength += effectiveSeconds;
		totalTracks++;
		
		Map<String, Double> errorsCurrent = cmp.getErrors();
		for (Entry<String, Double> entry : errorsCurrent.entrySet()) {
			String key = entry.getKey();
			Double value = entry.getValue();
			if (errors.containsKey(key)) {
				value += errors.get(key);
			}
			errors.put(key, value);
		}
	}

	public double getAOR() {
		return totalOverlap / totalTracks;
	}

	public double getWAOR() {
		return totalWeightedOverlap / totalEffectiveLength;
	}

	public double getAverageSegm() {
		return totalSegmentation / totalTracks;
	}

	public List<Entry<String, Double>> getErrors() {
		return MapUtil.sortMapByValue(errors, false);
	}
}
