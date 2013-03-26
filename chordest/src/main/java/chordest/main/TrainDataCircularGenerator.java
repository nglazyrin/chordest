package chordest.main;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
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
	private static final String CSV_FILE_B = PathConstants.OUTPUT_DIR + "train_dA_bass_c.csv";

	private final File chordFile;
	private final File bassFile;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList(TRAIN_FILE_LIST);
		TrainDataGenerator.deleteIfExists(CSV_FILE);
		TrainDataGenerator.deleteIfExists(CSV_FILE_B);
		int filesProcessed = 0;
		for (final String binFileName : tracklist) {
			TrainDataCircularGenerator tdg = new TrainDataCircularGenerator(CSV_FILE, CSV_FILE_B);
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
		result = DataUtil.smoothHorizontallyMedian(result, TrainDataGenerator.WINDOW);
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

	public TrainDataCircularGenerator(String chordCsvFileName, String bassCsvFileName) {
		chordFile = new File(chordCsvFileName);
		bassFile = new File(bassCsvFileName);
	}

	private void process(double[][] data, Chord[] chords) {
		if (data == null || chords == null) {
			LOG.error("data or chords is null");
			return;
		}
		try (OutputStream chordOut = FileUtils.openOutputStream(chordFile, true);
				OutputStream bassOut = FileUtils.openOutputStream(bassFile, true)) {
			for (int i = 0; i < data.length; i++) {
				if (data[i].length < TrainDataGenerator.INPUTS + 12) {
					throw new IOException("Spectrum bin length < " + (TrainDataGenerator.INPUTS+12) +  ": " + data[i].length);
				} else if (data[i].length > 72) {
					data[i] = ArrayUtils.subarray(data[i], 0, TrainDataGenerator.INPUTS + 12);
				}
				process(data[i], chords[i], chordOut, bassOut);
			}
		} catch (IOException e) {
			LOG.error("Error when writing result", e);
		}
	}

	private void process(double[] data, Chord chord, OutputStream chordOut, OutputStream bassOut) throws IOException {
		if (chord == null) {
			return;
		}
		if (chord.isEmpty()) {
			double[] dataLocal = Arrays.copyOfRange(data, TrainDataGenerator.OFFSET, TrainDataGenerator.OFFSET + TrainDataGenerator.INPUTS);
			chordOut.write(TrainDataGenerator.toByteArray(dataLocal, chord));
			bassOut.write(TrainDataGenerator.toByteArrayForBass(dataLocal, chord));
		} else {
			Note[] notes = chord.getNotes();
			for (int i = 0; i < 12; i++) {
				double[] dataLocal = Arrays.copyOfRange(data, TrainDataGenerator.OFFSET + i, TrainDataGenerator.OFFSET + i + TrainDataGenerator.INPUTS);
				Note[] newNotes = new Note[notes.length];
				for (int j = 0; j < notes.length; j++) {
					newNotes[j] = notes[j].withOffset(-i);
				}
				Chord chordLocal = new Chord(newNotes);
				chordOut.write(TrainDataGenerator.toByteArray(dataLocal, chordLocal));
				bassOut.write(TrainDataGenerator.toByteArrayForBass(dataLocal, chordLocal));
			}
		}
//		} else if (chord.isMajor()) {
//			Note startNote = chord.getRoot();
//			for (int i = 0; i < 12; i++) {
//				double[] dataLocal = Arrays.copyOfRange(data, i, i + TrainDataGenerator.INPUTS);
//				Chord chordLocal = Chord.major(startNote.withOffset(-i));
//				csvOut.write(TrainDataGenerator.toByteArray(dataLocal, chordLocal));
//			}
//		} else if (chord.isMinor()) {
//			Note startNote = chord.getRoot();
//			for (int i = 0; i < 12; i++) {
//				double[] dataLocal = Arrays.copyOfRange(data, i, i + TrainDataGenerator.INPUTS);
//				Chord chordLocal = Chord.minor(startNote.withOffset(-i));
//				csvOut.write(TrainDataGenerator.toByteArray(dataLocal, chordLocal));
//			}
//		}
	}

}
