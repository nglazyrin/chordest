package chordest.main;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.lab.LabFileReader;
import chordest.lab.LabSimilarity;
import chordest.util.MapUtil;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class AccuracyCalculator {

	private static final Logger LOG = LoggerFactory.getLogger(Roundtrip.class);

	private static final String EXPECTED_DIR = PathConstants.LAB_DIR + "Beatles" + PathConstants.SEP;

	private static final String ACTUAL_DIR = "result" + PathConstants.SEP + "toCheck" + PathConstants.SEP;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(new File(EXPECTED_DIR), "");
		double totalOverlap = 0;
		double totalWeightedOverlap = 0;
		int totalTracks = 0;
		double totalLength = 0;
		final Map<String, Double> errors = new HashMap<String, Double>();
		
		for (final String labFileName : tracklist) {
			LabFileReader expectedReader = new LabFileReader(new File(EXPECTED_DIR + labFileName));
			File actual = new File(ACTUAL_DIR + labFileName);
			if (! actual.exists()) {
				continue;
			}
			LabFileReader actualReader = new LabFileReader(actual);
			
			LabSimilarity sim = new LabSimilarity(
					expectedReader.getChords(), expectedReader.getTimestamps(),
					actualReader.getChords(), actualReader.getTimestamps());
			double overlap = sim.getOverlapMeasure();
			double effectiveSeconds = sim.getTotalSeconds();
			totalOverlap += overlap;
			totalWeightedOverlap += (overlap * effectiveSeconds);
			totalLength += effectiveSeconds;
			totalTracks++;
			
			Map<String, Double> errorsCurrent = sim.getErrors();
			for (Entry<String, Double> entry : errorsCurrent.entrySet()) {
				String key = entry.getKey();
				Double value = entry.getValue();
				if (errors.containsKey(key)) {
					value += errors.get(key);
				}
				errors.put(key, value);
			}
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			LOG.info(labFileName + ": " + overlap);
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//			SIM_LOG.info(labFileName.replace(',', '_').replace('\\', '/') + "," + ce.getKey() + "," +
//					overlap + "," + effectiveSeconds + "," + ce.getTotalSeconds());
		}

		double averageChordOverlapRatio = totalOverlap / totalTracks;
		double weightedAverageChordOverlapRatio = totalWeightedOverlap / totalLength;
		List<Entry<String, Double>> sorted = MapUtil.sortMapByValue(errors, false);
		logErrors(averageChordOverlapRatio, weightedAverageChordOverlapRatio, sorted, LOG);
	}

	private static void logErrors(double averageChordOverlapRatio,
			double weightedAverageChordOverlapRatio,
			List<Entry<String, Double>> sorted, Logger logger) {
		int top = 20;
		logger.info("");
		logger.info("Average chord overlap ratio: " + averageChordOverlapRatio);
		logger.info("Weighted average chord overlap ratio: " + weightedAverageChordOverlapRatio);
		logger.info(String.format("Errors TOP-%d:", top));
		for (int i = 0; i < top && i < sorted.size(); i ++) {
			logger.info(String.format("%s: %f", sorted.get(i).getKey(), sorted.get(i).getValue()));
		}
	}

}
