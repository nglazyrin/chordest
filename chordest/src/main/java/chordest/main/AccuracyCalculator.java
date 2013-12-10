package chordest.main;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.comparison.ChordListsComparison;
import chordest.chord.comparison.Triads;
import chordest.io.lab.LabFileReader;
import chordest.model.Chord;
import chordest.util.MapUtil;
import chordest.util.TracklistCreator;

/**
 * Calculates chord recognition quality (AOR and WAOR) using two given lists
 * of .lab files.
 * @author Nikolay
 *
 */
public class AccuracyCalculator {

	private static final Logger LOG = LoggerFactory.getLogger(Roundtrip.class);

	public static void main(String[] args) {
		if (args.length < 2) {
			LOG.error("Usage: AccuracyCalculator /path/to/expectedFileList.txt /path/to/actualFileList.txt");
			System.exit(1);
		}
		List<String> expectedTracklist = TracklistCreator.readTrackList(args[0]);
		List<String> actualTracklist = TracklistCreator.readTrackList(args[1]);
		if (expectedTracklist.size() != actualTracklist.size()) {
			LOG.error("Expected and actual file lists have different lengths");
			System.exit(2);
		}
		
		double totalOverlap = 0;
		double totalWeightedOverlap = 0;
		int totalTracks = 0;
		double totalLength = 0;
		final Map<Pair<Chord, Chord>, Double> errors = new HashMap<Pair<Chord, Chord>, Double>();
		
		for (int i = 0; i < expectedTracklist.size(); i++) {
			File expected = new File(expectedTracklist.get(i));
			LabFileReader expectedReader = new LabFileReader(expected);
			LabFileReader actualReader = new LabFileReader(new File(actualTracklist.get(i)));
			
			ChordListsComparison sim = new ChordListsComparison(
					expectedReader.getChords(), expectedReader.getTimestamps(),
					actualReader.getChords(), actualReader.getTimestamps(), new Triads());
			double overlap = sim.getOverlapMeasure();
			double effectiveSeconds = sim.getEffectiveSeconds();
			totalOverlap += overlap;
			totalWeightedOverlap += (overlap * effectiveSeconds);
			totalLength += effectiveSeconds;
			totalTracks++;
			
			Map<Pair<Chord, Chord>, Double> errorsCurrent = sim.getErrors();
			for (Entry<Pair<Chord, Chord>, Double> entry : errorsCurrent.entrySet()) {
				Pair<Chord, Chord> key = entry.getKey();
				Double value = entry.getValue();
				if (errors.containsKey(key)) {
					value += errors.get(key);
				}
				errors.put(key, value);
			}
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			LOG.info(expected.getName() + ": " + overlap);
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//			SIM_LOG.info(labFileName.replace(',', '_').replace('\\', '/') + "," + ce.getKey() + "," +
//					overlap + "," + effectiveSeconds + "," + ce.getTotalSeconds());
		}

		double averageChordOverlapRatio = totalOverlap / totalTracks;
		double weightedAverageChordOverlapRatio = totalWeightedOverlap / totalLength;
		List<Entry<Pair<Chord, Chord>, Double>> sorted = MapUtil.sortMapByValue(errors, false);
		logErrors(averageChordOverlapRatio, weightedAverageChordOverlapRatio, sorted, LOG);
	}

	private static void logErrors(double averageChordOverlapRatio,
			double weightedAverageChordOverlapRatio,
			List<Entry<Pair<Chord, Chord>, Double>> sorted, Logger logger) {
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
