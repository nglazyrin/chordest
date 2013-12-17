package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordExtractor;
import chordest.chord.comparison.ChordListsComparison;
import chordest.chord.comparison.ComparisonAccumulator;
import chordest.chord.comparison.Mirex2010;
import chordest.chord.comparison.Tetrads;
import chordest.chord.comparison.Triads;
import chordest.configuration.Configuration;
import chordest.io.AbstractWriter;
import chordest.io.csv.CsvFileWriter;
import chordest.io.csv.CsvSpectrumFileWriter;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.spectrum.FileSpectrumDataProvider;
import chordest.spectrum.WaveFileSpectrumDataProvider;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

/**
 * This class is mostly used to estimate chord recognition quality during
 * development. Works with pre-calculated spectra to make it faster.
 * @author Nikolay
 */
public class Roundtrip {

	private static final Logger LOG = LoggerFactory.getLogger(Roundtrip.class);
	private static final Logger ERR_LOG = LoggerFactory.getLogger("Errors");
	private static final Logger SIM_LOG = LoggerFactory.getLogger("Similarity");

	private static final String SEP = PathConstants.SEP;
	public static final String CSV_ACTUAL_DIR = PathConstants.CSV_DIR + "actual" + SEP;
	public static final String CSV_CHROMA_DIR = PathConstants.CSV_DIR + "chroma" + SEP;
	public static final String CSV_EXPECTED_DIR = PathConstants.CSV_DIR + "expected" + SEP;
	public static final String FILE_LIST = PathConstants.RESOURCES_DIR + "filelists" + SEP + "bqrz_bin.txt";

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList(FILE_LIST);
		ComparisonAccumulator[] acc = new ComparisonAccumulator[] {
			new ComparisonAccumulator(new Mirex2010()),
			new ComparisonAccumulator(new Triads()),
			new ComparisonAccumulator(new Tetrads())
		};
		Configuration c = new Configuration();
		ERR_LOG.info("metric;type;total;0;1;2;3;4;5;6;7;8;9;10;11");
		SIM_LOG.info("name,key,overlapM,overlap3,overlap4,segmentation,effective_length,full_length");
		for (final String binFileName : tracklist) {
			String temp = StringUtils.substringAfterLast(binFileName, PathConstants.SEP);
			String track = StringUtils.substringBeforeLast(temp, PathConstants.EXT_WAV + PathConstants.EXT_BIN);
			String labFileName = track + PathConstants.EXT_LAB;
			final String csvFileName = track + PathConstants.EXT_CSV;
			ChordExtractor ce;
			if (new File(binFileName).exists()) {
				ce = new ChordExtractor(c.process, new FileSpectrumDataProvider(binFileName));
			} else {
				final String wavFileName = PathConstants.WAV_DIR + track + PathConstants.EXT_WAV;
				final String beatFileName = PathConstants.BEAT_DIR + track + PathConstants.EXT_BEAT;
				ce = new ChordExtractor(c.process, new WaveFileSpectrumDataProvider(wavFileName, beatFileName, c));
			}

			write(new LabFileWriter(ce), PathConstants.OUTPUT_DIR + labFileName);
			write(new CsvSpectrumFileWriter(ce.getChroma()), CSV_CHROMA_DIR + csvFileName);

			LabFileReader labReaderActual = new LabFileReader(new File(PathConstants.OUTPUT_DIR + labFileName));
			write(new CsvFileWriter(ce.getChords(), ce.getOriginalBeatTimes()), CSV_ACTUAL_DIR + csvFileName);

			LabFileReader labReaderExpected = new LabFileReader(new File(PathConstants.LAB_DIR + labFileName));
			write(new CsvFileWriter(labReaderExpected.getChords(), labReaderExpected.getTimestamps()), CSV_EXPECTED_DIR + csvFileName);

			ChordListsComparison[] cmp = new ChordListsComparison[3];
			for (int i = 0; i < acc.length; i++) {
				cmp[i] = new ChordListsComparison(labReaderExpected.getChords(),
						labReaderExpected.getTimestamps(), labReaderActual.getChords(),
						labReaderActual.getTimestamps(), acc[i].getMetric());
				acc[i].append(cmp[i]);
			}
			
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			LOG.info(String.format("%s: Mirex %.4f; Triads %.4f; Tetrads %.4f; Segm %.4f", 
					track, cmp[0].getOverlapMeasure(), cmp[1].getOverlapMeasure(),
					cmp[2].getOverlapMeasure(), cmp[0].getSegmentation()));
			LOG.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			SIM_LOG.info(String.format("%s;%s;%f;%f;%f;%f;%f;%f", 
					track.replace(',', '_').replace('\\', '/'), null,
					cmp[0].getOverlapMeasure(),
					cmp[1].getOverlapMeasure(),
					cmp[2].getOverlapMeasure(),
					cmp[0].getSegmentation(),
					cmp[2].getEffectiveSeconds(),
					cmp[0].getTotalSeconds()));
		}
		
		for (int i = 0; i < acc.length; i++) {
			LOG.info("");
			acc[i].printStatistics(LOG);
			acc[i].printErrorStatistics(ERR_LOG);
		}
	}

	public static void write(AbstractWriter writer, String fileName) {
		try {
			writer.writeTo(new File(fileName));
		} catch (IOException e) {
			LOG.error("Error when writing to " + fileName, e);
		}
	}

}
