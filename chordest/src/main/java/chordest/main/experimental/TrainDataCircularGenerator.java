package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.spectrum.SpectrumFileReader;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
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
	private static final String TRAIN_FILE_LIST = PathConstants.RESOURCES_DIR + "filelists" + PathConstants.SEP + "bqz_bin1train.txt";
	private static final String CSV_FILE = PathConstants.OUTPUT_DIR + "train_dA_c.csv";

	private static final boolean SEQUENTIAL = false;

	private final File chordFile;

	private int majors = 0;
	private int minors = 0;

	private static final double DELTA = 0.5;
	private static final int L = 100;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList(TRAIN_FILE_LIST);
		TrainDataGenerator.deleteIfExists(CSV_FILE);
		int filesProcessed = 0;

		for (final String binFileName : tracklist) {
			TrainDataCircularGenerator tdg = new TrainDataCircularGenerator(CSV_FILE);
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] result = TrainDataGenerator.prepareSpectrum(sd);
			Chord[] chords = TrainDataGenerator.prepareChords(binFileName, sd, DELTA);
			tdg.processFile(result, chords, 12);
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

	public TrainDataCircularGenerator(String chordCsvFileName) {
		chordFile = new File(chordCsvFileName);
	}

	private void processFile(double[][] data, Chord[] chords, int notesInOctave) {
		if (data == null || chords == null) {
			LOG.error("data or chords is null");
			return;
		}
		if (SEQUENTIAL) {
			for (int offset = 0; offset < 12; offset++) {
				try (OutputStream chordOut = FileUtils.openOutputStream(chordFile, true)) {
					for (int i = 0; i < data.length; i++) {
						int desiredLength = TrainDataGenerator.INPUTS + notesInOctave;
						if (data[i].length < desiredLength) {
							throw new IOException("Spectrum bin length < " + desiredLength +  ": " + data[i].length);
						} else if (data[i].length > desiredLength) {
							data[i] = ArrayUtils.subarray(data[i], 0, desiredLength);
						}
						processColumn(data[i], chords[i], offset * notesInOctave / 12, chordOut);
					}
				} catch (IOException e) {
					LOG.error("Error when writing result", e);
				}
			}
		} else {
			for (int i = 0; i < data.length; i++) {
				Chord chord = chords[i];
				if (chord == null || chord.isEmpty()) {
					// do nothing
				} else if (chord.equalsToTriad(Chord.major(chord.getRoot()))) {
					chord = Chord.major(chord.getRoot());
					if (majors - minors < L) {
						majors++;
					} else {
						continue;
					}
				} else if (chord.equalsToTriad(Chord.minor(chord.getRoot()))) {
					chord = Chord.minor(chord.getRoot());
					if (minors - majors < L) {
						minors++;
					} else {
						continue;
					}
				}
				if (Math.abs(majors - minors) < L) {
					try (OutputStream chordOut = FileUtils.openOutputStream(chordFile, true)) {
						for (int offset = 0; offset < 12; offset++) {
							int desiredLength = TrainDataGenerator.INPUTS + notesInOctave;
							if (data[i].length < desiredLength) {
								throw new IOException("Spectrum bin length < " + desiredLength +  ": " + data[i].length);
							} else if (data[i].length > desiredLength) {
								data[i] = ArrayUtils.subarray(data[i], 0, desiredLength);
							}
							processColumn(data[i], chords[i], offset * notesInOctave / 12, chordOut);
						}
					} catch (IOException e) {
						LOG.error("Error when writing result", e);
					}
				}
			}
		}
		
	}

	private void processColumn(double[] data, Chord chord, int offset, OutputStream chordOut) throws IOException {
		if (chord == null) {
			return;
		}
		double[] dataLocal = Arrays.copyOfRange(data, TrainDataGenerator.OFFSET + offset, TrainDataGenerator.OFFSET + offset + TrainDataGenerator.INPUTS);
		if (chord.isEmpty()) {
			chordOut.write(TrainDataGenerator.toByteArray(dataLocal, chord));
		} else if (chord.equalsToTriad(Chord.major(chord.getRoot())) || chord.equalsToTriad(Chord.minor(chord.getRoot()))) {
			Chord chordLocal = rotateChord(chord, offset);
			chordOut.write(TrainDataGenerator.toByteArray(dataLocal, chordLocal));
		}

	}

	private Chord rotateChord(Chord chord, int offset) {
		Note[] notes = chord.getNotes();
		Note[] newNotes = new Note[notes.length];
		for (int j = 0; j < notes.length; j++) {
			newNotes[j] = notes[j].withOffset(-offset);
		}
		Chord chordLocal = new Chord(newNotes);
		return chordLocal;
	}

}
