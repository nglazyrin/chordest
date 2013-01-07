package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordExtractor;
import chordest.chord.ChordListsComparison;
import chordest.chord.ComparisonAccumulator;
import chordest.configuration.Configuration;
import chordest.io.AbstractWriter;
import chordest.io.csv.CsvFileWriter;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.spectrum.FileSpectrumDataProvider;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class Roundtrip {

	private static final Logger LOG = LoggerFactory.getLogger(Roundtrip.class);
	private static final Logger SIM_LOG = LoggerFactory.getLogger("Similarity");

	private static final String SEP = PathConstants.SEP;
	private static final String CSV_ACTUAL_DIR = PathConstants.CSV_DIR + "actual" + SEP;
	private static final String CSV_EXPECTED_DIR = PathConstants.CSV_DIR + "expected" + SEP;
	private static final String LAB_DIR = PathConstants.LAB_DIR;
	private static final String SPECTRUM_DIR = "spectrum8" + SEP;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(new File(LAB_DIR), "");
		ComparisonAccumulator acc = new ComparisonAccumulator();
		Configuration c = new Configuration();
		SIM_LOG.info("name,key,overlap,effective_length,full_length");
		for (final String labFileName : tracklist) {
			final String csvFileName = labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_CSV);
			final String spectrumFileName = SPECTRUM_DIR + 
					labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_WAV) + PathConstants.EXT_BIN;
			ChordExtractor ce;
			if (new File(spectrumFileName).exists()) {
				ce = new ChordExtractor(c.process, new FileSpectrumDataProvider(spectrumFileName));
			} else {
				final String wavFileName = PathConstants.WAV_DIR + 
						labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_WAV);
				ce = new ChordExtractor(c.process, new WaveFileSpectrumDataProvider(wavFileName, c.spectrum));
			}

			write(new LabFileWriter(ce), PathConstants.OUTPUT_DIR + labFileName);

			LabFileReader labReaderActual = new LabFileReader(new File(PathConstants.OUTPUT_DIR + labFileName));
			write(new CsvFileWriter(ce.getChords(), ce.getOriginalBeatTimes()), CSV_ACTUAL_DIR + csvFileName);

			LabFileReader labReaderExpected = new LabFileReader(new File(PathConstants.LAB_DIR + labFileName));
			write(new CsvFileWriter(labReaderExpected.getChords(), labReaderExpected.getTimestamps()), CSV_EXPECTED_DIR + csvFileName);

			ChordListsComparison sim = new ChordListsComparison(labReaderExpected.getChords(),
					labReaderExpected.getTimestamps(), labReaderActual.getChords(), labReaderActual.getTimestamps());
			acc.append(sim);
			
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			LOG.info(labFileName + ": " + sim.getOverlapMeasure());
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			SIM_LOG.info(labFileName.replace(',', '_').replace('\\', '/') + "," + ce.getKey() + "," +
					sim.getOverlapMeasure() + "," + sim.getTotalSeconds() + "," + ce.getSpectrum().totalSeconds);
		}
		
		LOG.info("Test finished");
		logErrors(acc, LOG);
//		logErrors(acc, SIM_LOG);
	}

	private static void logErrors(ComparisonAccumulator acc, Logger logger) {
		int top = 20;
		logger.info("");
		logger.info("Average chord overlap ratio: " + acc.getAOR());
		logger.info("Weighted average chord overlap ratio: " + acc.getWAOR());
		logger.info(String.format("Errors TOP-%d:", top));
		List<Entry<String, Double>> errors = acc.getErrors();
		for (int i = 0; i < top && i < errors.size(); i++) {
			logger.info(String.format("%s: %f", errors.get(i).getKey(), errors.get(i).getValue()));
		}
	}

	private static void write(AbstractWriter writer, String fileName) {
		try {
			writer.writeTo(new File(fileName));
		} catch (IOException e) {
			LOG.error("Error when writing to " + fileName, e);
		}
	}

}
