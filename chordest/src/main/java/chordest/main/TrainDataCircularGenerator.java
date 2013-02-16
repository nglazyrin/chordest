package chordest.main;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.configuration.Configuration;
import chordest.io.lab.LabFileReader;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
import chordest.util.DataUtil;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

/**
 * For each 72-dimensional spectrum bin corresponding to a major/minor chord
 * take 12 samples using 60 element window sliding from lowest to highest bin.
 * Treat each sample as corresponding to a major/minor chord with moving root
 * note. The resulting training data contains equal number of instances for
 * each root note, but different for major and minor chords.
 * 
 * @author Nikolay
 *
 */
public class TrainDataCircularGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(TrainDataGenerator.class);
	private static final String TRAIN_FILE_LIST = "work" + PathConstants.SEP + "all_files0.txt";
	private static final String CSV_FILE = PathConstants.OUTPUT_DIR + "train_dA_c.csv";

	private OutputStream csvOut;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList(TRAIN_FILE_LIST);
		TrainDataGenerator.deleteIfExists(CSV_FILE);
		int filesProcessed = 0;
		for (final String binFileName : tracklist) {
			TrainDataCircularGenerator tdg = new TrainDataCircularGenerator(CSV_FILE, true);
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] result = TrainDataCircularGenerator.prepareSpectrum(sd);
			Chord[] chords = TrainDataCircularGenerator.prepareChords(binFileName, sd);
			tdg.process(result, chords);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed. Result was saved to " + CSV_FILE);
	}

	public static void deleteIfExists(String fileName) {
		File resultFile = new File(fileName);
		if (resultFile.exists()) {
			try {
				FileUtils.forceDelete(resultFile);
			} catch (IOException e) {
				LOG.warn("Error when deleting file " + fileName, e);
			}
		}
	}

	public static double[][] prepareSpectrum(final SpectrumData sd) {
		double[][] result = sd.spectrum;
		int window = new Configuration().process.medianFilterWindow;
		result = DataUtil.smoothHorizontallyMedian(result, window);
		result = DataUtil.shrink(result, sd.framesPerBeat);
		result = DataUtil.toLogSpectrum(result);
		result = DataUtil.reduce(result, sd.scaleInfo.getOctavesCount());
		DataUtil.scaleEachTo01(result);
		return result;
	}

	public static Chord[] prepareChords(final String binFileName, final SpectrumData sd) {
		String track = StringUtils.substringAfterLast(binFileName, PathConstants.SEP);
		String labFileName = PathConstants.LAB_DIR + track.replace(PathConstants.EXT_WAV + PathConstants.EXT_BIN, PathConstants.EXT_LAB);
		LabFileReader labReader = new LabFileReader(new File(labFileName));
		Chord[] result = new Chord[sd.beatTimes.length - 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = labReader.getChord(sd.beatTimes[i]);
		}
		return result;
	}

	public TrainDataCircularGenerator(String outputCsvFileName, boolean append) {
		File file = new File(outputCsvFileName);
		try {
			csvOut = FileUtils.openOutputStream(file, append);
		} catch (IOException e) {
			LOG.error("Error when creating resulting .csv file", e);
			System.exit(-2);
		}
	}

	private void process(double[][] data, Chord[] chords) {
		if (data == null || chords == null) {
			LOG.error("data or chords is null");
			return;
		}
		try {
			for (int i = 0; i < data.length; i++) {
				if (data[i].length != 72) {
					throw new IOException("Spectrum bin length != 72: " + data[i].length);
				}
				process(data[i], chords[i]);
			}
		} catch (IOException e) {
			LOG.error("Error when writing result", e);
		} finally {
			try {
				csvOut.close();
			} catch (IOException e) {
				LOG.error("Error when closing output stream for the resulting file", e);
			}
		}
	}

	private void process(double[] data, Chord chord) throws IOException {
		if (chord == null) {
			return;
		}
		if (chord.isEmpty()) {
			double[] dataLocal = Arrays.copyOfRange(data, 0, 60);
			csvOut.write(TrainDataGenerator.toByteArray(dataLocal, chord));
		} else if (chord.isMajor()) {
			Note startNote = chord.getRoot();
			for (int i = 0; i < 12; i++) {
				double[] dataLocal = Arrays.copyOfRange(data, i, i + 60);
				Chord chordLocal = Chord.major(startNote.withOffset(-i));
				csvOut.write(TrainDataGenerator.toByteArray(dataLocal, chordLocal));
			}
		} else if (chord.isMinor()) {
			Note startNote = chord.getRoot();
			for (int i = 0; i < 12; i++) {
				double[] dataLocal = Arrays.copyOfRange(data, i, i + 60);
				Chord chordLocal = Chord.minor(startNote.withOffset(-i));
				csvOut.write(TrainDataGenerator.toByteArray(dataLocal, chordLocal));
			}
		}
	}

}
