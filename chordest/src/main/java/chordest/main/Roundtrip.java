package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.BeatRootAdapter;
import chordest.chord.ChordExtractor;
import chordest.lab.CsvFileWriter;
import chordest.lab.LabFileReader;
import chordest.lab.LabFileWriter;
import chordest.lab.LabSimilarity;
import chordest.properties.Configuration;
import chordest.util.MapUtil;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class Roundtrip {

	private static final Logger LOG = LoggerFactory.getLogger(Roundtrip.class);
	private static final Logger SIM_LOG = LoggerFactory.getLogger("Similarity");

	private static final String SEP = PathConstants.SEP;
//	private static final String ARTIST = "Beatles";
//	private static final String ALBUM = "10CD1_-_The_Beatles";
//	private static final String PREFIX = ARTIST + SEP + ALBUM + SEP;
//	private static final String PREFIX = ARTIST + SEP;
	private static final String CSV_ACTUAL_DIR = PathConstants.CSV_DIR + "actual" + SEP;
	private static final String CSV_EXPECTED_DIR = PathConstants.CSV_DIR + "expected" + SEP;
	private static final String LAB_DIR = PathConstants.LAB_DIR;
	private static final String SPECTRUM_DIR = "spectrum_tuning" + SEP; // step 0

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(new File(LAB_DIR), "");
		Configuration c = new Configuration("config" + SEP + "parameters.properties");
		double totalOverlap = 0;
		double totalWeightedOverlap = 0;
		int totalTracks = 0;
		double totalLength = 0;
		final Map<String, Double> errors = new HashMap<String, Double>();
		SIM_LOG.info("name,key,overlap,effective_length,full_length");
		for (final String labFileName : tracklist) {
			final String csvFileName = labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_CSV);
			final String spectrumFileName = SPECTRUM_DIR + 
					labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_BIN);
//			BeatRootAdapter beatRoot = new BeatRootAdapter(wavFileName, beatFileName);
			ChordExtractor ce;
			if (new File(spectrumFileName).exists()) {
				ce = new ChordExtractor(c, spectrumFileName);
			} else {
				final String beatFileName = PathConstants.BEAT_DIR +
						labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_BEAT);
				final String wavFileName = PathConstants.WAV_DIR + 
						labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_WAV);
				ce = new ChordExtractor(c, wavFileName, new BeatRootAdapter(wavFileName, beatFileName));
			}

			double[] beatTimes = ce.getOriginalBeatTimes();
			LabFileWriter labWriter = new LabFileWriter(ce.getChords(), beatTimes);
			try {
				labWriter.writeTo(new File(PathConstants.OUTPUT_DIR + labFileName));
			} catch (IOException e) {
				LOG.error("Error when saving lab file", e);
			}
			CsvFileWriter csvWriter = new CsvFileWriter(ce.getChords(), beatTimes);
			try {
				csvWriter.writeTo(new File(CSV_ACTUAL_DIR + csvFileName));
			} catch (IOException e) {
				LOG.error("Error when saving actual csv file", e);
			}

			LabFileReader labReader = new LabFileReader(new File(
					PathConstants.LAB_DIR + labFileName));
			csvWriter = new CsvFileWriter(labReader.getChords(), labReader.getTimestamps());
			try {
				csvWriter.writeTo(new File(CSV_EXPECTED_DIR + csvFileName));
			} catch (IOException e) {
				LOG.error("Error when saving expected csv file", e);
			}
			
			LabSimilarity sim = new LabSimilarity(labReader.getChords(),
					labReader.getTimestamps(), ce.getChords(), ce.getOriginalBeatTimes());
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
			SIM_LOG.info(labFileName.replace(',', '_').replace('\\', '/') + "," + ce.getKey() + "," +
					overlap + "," + effectiveSeconds + "," + ce.getTotalSeconds());
		}
		
		LOG.info("Test finished");
		double averageChordOverlapRatio = totalOverlap / totalTracks;
		double weightedAverageChordOverlapRatio = totalWeightedOverlap / totalLength;
		List<Entry<String, Double>> sorted = MapUtil.sortMapByValue(errors, false);
		logErrors(averageChordOverlapRatio, weightedAverageChordOverlapRatio, sorted, LOG);
//		logErrors(averageChordOverlapRatio, weightedAverageChordOverlapRatio, sorted, SIM_LOG);
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
