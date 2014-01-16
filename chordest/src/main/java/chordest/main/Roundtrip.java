package chordest.main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordRecognizer;
import chordest.chord.ChromaExtractor;
import chordest.chord.comparison.ChordListsComparison;
import chordest.chord.comparison.ComparisonAccumulator;
import chordest.chord.comparison.Mirex2010;
import chordest.chord.comparison.Tetrads;
import chordest.chord.comparison.Triads;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.io.AbstractWriter;
import chordest.io.csv.CsvFileWriter;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.model.Chord;
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

	protected static final Logger LOG = LoggerFactory.getLogger(Roundtrip.class);
	protected static final Logger ERR_LOG = LoggerFactory.getLogger("Errors");
	protected static final Logger SIM_LOG = LoggerFactory.getLogger("Similarity");

	private static final String SEP = PathConstants.SEP;
	public static final String CSV_TRIADS_DIR = PathConstants.CSV_DIR + "triads" + SEP;
	public static final String CSV_TETRADS_DIR = PathConstants.CSV_DIR + "tetrads" + SEP;
	public static final String CSV_EXPECTED_DIR = PathConstants.CSV_DIR + "expected" + SEP;
	public static final String LAB_TRIADS_DIR = PathConstants.OUTPUT_DIR + "triads" + SEP;
	public static final String LAB_TETRADS_DIR = PathConstants.OUTPUT_DIR + "tetrads" + SEP;
	public static final String FILE_LIST = PathConstants.RESOURCES_DIR + "filelists" + SEP + "bqrz_bin.txt";

	protected static final ComparisonAccumulator[] acc = new ComparisonAccumulator[] {
		new ComparisonAccumulator(new Mirex2010()),
		new ComparisonAccumulator(new Triads()),
		new ComparisonAccumulator(new Tetrads())
	};

	protected static Configuration c;

	public static void main(String[] args) {
		List<String> tracklist;
		String labDir;
		if (args.length > 2) {
			c = new Configuration(args[0]);
			tracklist = TracklistCreator.readTrackList(args[1]);
			labDir = args[2];
		} else {
			c = new Configuration();
			tracklist = TracklistCreator.readTrackList(FILE_LIST);
			labDir = PathConstants.LAB_DIR;
		}
		writeCsvHeaders();
		for (final String binFileName : tracklist) {
			String temp = StringUtils.substringAfterLast(binFileName, PathConstants.SEP);
			String track = StringUtils.substringBeforeLast(temp, PathConstants.EXT_WAV + PathConstants.EXT_BIN);
			ChromaExtractor ce;
			if (new File(binFileName).exists()) {
				ce = new ChromaExtractor(c.process, c.template, new FileSpectrumDataProvider(binFileName),
						c.spectrum.framesPerBeat);
			} else {
				final String wavFileName = PathConstants.WAV_DIR + track + PathConstants.EXT_WAV;
				final String beatFileName = PathConstants.BEAT_DIR + track + PathConstants.EXT_BEAT;
				ce = new ChromaExtractor(c.process, c.template, new WaveFileSpectrumDataProvider(wavFileName, beatFileName, c));
			}
			ITemplateProducer producer = new TemplateProducer(ce.getStartNote(), c.template);

			processFile(ce.getChroma(), ce.getNoChordness(), ce.getOriginalBeatTimes(),
					labDir, track, acc, producer);
		}
		
		writeStatistics();
	}

	protected static void writeCsvHeaders() {
		ERR_LOG.info("metric;type;total;0;1;2;3;4;5;6;7;8;9;10;11");
		SIM_LOG.info("name;key;overlapM;overlap3;overlap4;segmentation;effective_length;full_length");
	}

	protected static void processFile(double[][] chroma, double[] noChordness,
			double[] beatTimes, String labDir, String track,
			ComparisonAccumulator[] acc, ITemplateProducer producer) {
		final String csvFileName = track + PathConstants.EXT_CSV;
		final String labFileName = track + PathConstants.EXT_LAB;
		ChordRecognizer cr = new ChordRecognizer(chroma, noChordness, producer, c.process.noChordnessLimit);
		Chord[] triads = cr.recognize(new Triads().getOutputTypes());
		Chord[] tetrads = cr.recognize(new Tetrads().getOutputTypes());
		
		saveFiles(LAB_TRIADS_DIR + labFileName, CSV_TRIADS_DIR + csvFileName, triads, beatTimes);
		saveFiles(LAB_TETRADS_DIR + labFileName, CSV_TETRADS_DIR + csvFileName, tetrads, beatTimes);
		LabFileReader labReaderExpected = new LabFileReader(new File(labDir + labFileName));
		write(new CsvFileWriter(labReaderExpected.getChords(),
				labReaderExpected.getTimestamps()), CSV_EXPECTED_DIR + csvFileName);
		
		ChordListsComparison[] cmp = new ChordListsComparison[acc.length];
		for (int i = 0; i < acc.length; i++) {
			LabFileReader labReaderActual = new LabFileReader(
					new File((acc[i].getMetric() instanceof Tetrads ?
							LAB_TETRADS_DIR : LAB_TRIADS_DIR) + labFileName));
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

	protected static void writeStatistics() {
		for (int i = 0; i < acc.length; i++) {
			LOG.info("");
			acc[i].printStatistics(LOG);
			acc[i].printErrorStatistics(ERR_LOG);
		}
	}

	private static void saveFiles(String labFilePath, String csvFilePath, Chord[] chords, double[] beatTimes) {
		write(new LabFileWriter(chords, beatTimes), labFilePath);
		write(new CsvFileWriter(chords, beatTimes), csvFilePath);
	}

	protected static void write(AbstractWriter writer, String fileName) {
		try {
			writer.writeTo(new File(fileName));
		} catch (IOException e) {
			LOG.error("Error when writing to " + fileName, e);
		}
	}

}
