package chordest.main;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.ChordRecognizer;
import chordest.chord.ChromaExtractor;
import chordest.chord.comparison.ChordListsComparison;
import chordest.chord.comparison.IEvaluationMetric;
import chordest.chord.comparison.Triads;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.model.Chord;
import chordest.spectrum.FileSpectrumDataProvider;
import chordest.spectrum.WaveFileSpectrumDataProvider;

/**
 * Performs chord estimation only for a given file. Spectrum can be given as
 * a .bin file, otherwise it will also be computed.
 * @author Nikolay
 *
 */
public class SingleFile {

	private static final Logger LOG = LoggerFactory.getLogger(SingleFile.class);

	public static void main(String[] args) {
		if (args.length < 1) {
			LOG.error("Usage: SingleFile /path/to/testFile.wav [/path/to/testFileSpectrum.bin /path/to/testFile.lab /path/to/results/]");
			System.exit(-1);
		}
		String WAV_FILENAME = args[0];
		String SPECTRUM_FILENAME = null;
		if (args.length > 1) {
			SPECTRUM_FILENAME = args[1];
		}
		String LAB_FILENAME = null;
		if (args.length > 2) {
			LAB_FILENAME = args[2];
		}
		String RESULT_PATH = new File(WAV_FILENAME + ".lab").getAbsolutePath();
		if (args.length > 3) {
			RESULT_PATH = args[3];
		}
		String BEAT_FILENAME = new File(WAV_FILENAME + ".beat.txt").getAbsolutePath();
		
		Configuration c = new Configuration();
		IEvaluationMetric metric = new Triads();
		ChromaExtractor ce;
		if (SPECTRUM_FILENAME != null && new File(SPECTRUM_FILENAME).exists()) {
			ce = new ChromaExtractor(c.process, c.template, 
					new FileSpectrumDataProvider(SPECTRUM_FILENAME));
		} else {
			ce = new ChromaExtractor(c.process, c.template,
					new WaveFileSpectrumDataProvider(WAV_FILENAME, BEAT_FILENAME, c));
		}
		ITemplateProducer producer = new TemplateProducer(ce.getStartNote(), c.template);
		ChordRecognizer cr = new ChordRecognizer(ce.getChroma(), ce.getNoChordness(), producer, c.process.noChordnessLimit, ce.getKey());
		Chord[] chords = cr.recognize(metric.getOutputTypes());

		if (LAB_FILENAME != null) {
			File labFile = new File(LAB_FILENAME);
			if (labFile.exists()) {
				LabFileReader labReader = new LabFileReader(labFile);
				ChordListsComparison cmp = new ChordListsComparison(labReader.getChords(),
						labReader.getTimestamps(), chords, ce.getOriginalBeatTimes(), metric);
				LOG.info("Overlap measure: " + cmp.getOverlapMeasure());
				LOG.info("Key: " + ce.getKey());
			}
		}
		
		int startOffset = c.spectrum.offsetFromF0InSemitones;
//		Visualizer.visualizeChords(chords, ce.getOriginalBeatTimes(), WAV_FILENAME, startOffset);
		
		LabFileWriter labWriter = new LabFileWriter(chords, ce.getOriginalBeatTimes());
		try {
			labWriter.writeTo(new File(RESULT_PATH));
		} catch (IOException e) {
			LOG.error("Error when writing lab file", e);
		}
		
//		CsvFileWriter csvWriter = new CsvFileWriter(ce.getChords(), beatTimes);
//		try {
//			csvWriter.writeTo(new File(PathConstants.OUTPUT_DIR + csvFileName));
//		} catch (IOException e) {
//			LOG.error("Error when writing csv file", e);
//		}
	}

}
