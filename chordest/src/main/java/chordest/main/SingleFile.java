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
import chordest.util.Visualizer;

/**
 * Performs chord estimation only for a given file. Spectrum can be given as
 * a .bin file, otherwise it will also be computed.
 * @author Nikolay
 *
 */
public class SingleFile {

//	private static final String SEP = PathConstants.SEP;
//	private static final String ARTIST = "Beatles";
//	private static final String ALBUM = "05_-_Help!";
//	private static final String TRACK = "08_-_Act_Naturally";
//	private static final String TRACK_PATH = ARTIST + SEP + ALBUM + SEP + TRACK + PathConstants.EXT_WAV;
	private static final Logger LOG = LoggerFactory.getLogger(SingleFile.class);

	public static void main(String[] args) {
		if (args.length != 4) {
			LOG.error("Usage: SingleFile /path/to/testFile.wav /path/to/testFileSpectrum.bin /path/to/testFile.lab /path/to/results/");
			System.exit(-1);
		}
		String WAV_FILENAME = args[0];
		String SPECTRUM_FILENAME = args[1];
		String LAB_FILENAME = args[2];
		String RESULT_PATH = args[3];
		File wavFile = new File(WAV_FILENAME);
		String RESULT_FILENAME = RESULT_PATH + wavFile.getName() + ".txt";
		String BEAT_FILENAME = RESULT_PATH + wavFile.getName() + ".beat.txt";
		
		Configuration c = new Configuration();
		IEvaluationMetric metric = new Triads();
		File spectrumFile = new File(SPECTRUM_FILENAME);
		ChromaExtractor ce;
		if (spectrumFile.exists()) {
			ce = new ChromaExtractor(c.process, c.template, new FileSpectrumDataProvider(SPECTRUM_FILENAME));
		} else {
			ce = new ChromaExtractor(c.process, c.template,
					new WaveFileSpectrumDataProvider(WAV_FILENAME, BEAT_FILENAME, c));
		}
		ITemplateProducer producer = new TemplateProducer(ce.getStartNote(), c.template);
		ChordRecognizer cr = new ChordRecognizer(ce.getChroma(), ce.getNoChordness(), producer);
		Chord[] chords = cr.recognize(metric.getOutputTypes());

		File labFile = new File(LAB_FILENAME);
		if (labFile.exists()) {
			LabFileReader labReader = new LabFileReader(labFile);
			ChordListsComparison cmp = new ChordListsComparison(labReader.getChords(),
					labReader.getTimestamps(), chords, ce.getOriginalBeatTimes(), metric);
			LOG.info("Overlap measure: " + cmp.getOverlapMeasure());
			LOG.info("Key: " + ce.getKey());
		}
		
		int startOffset = c.spectrum.offsetFromF0InSemitones;
		Visualizer.visualizeChords(chords, ce.getOriginalBeatTimes(), WAV_FILENAME, startOffset);
		
		LabFileWriter labWriter = new LabFileWriter(chords, ce.getOriginalBeatTimes());
		try {
			labWriter.writeTo(new File(RESULT_FILENAME));
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
