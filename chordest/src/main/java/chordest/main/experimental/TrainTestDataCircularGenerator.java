package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class TrainTestDataCircularGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(TrainDataGenerator.class);
	private static final String PREFIX = PathConstants.RESOURCES_DIR + "filelists" + PathConstants.SEP;
	
	private static final String TRAIN_FILE_LIST = PREFIX + "bqz_bin0train.txt";
	public static final String TEST_FILE_LIST = PREFIX + "bqz_bin0test.txt";

	private static final boolean EXTRA_OCTAVE = true;
	private static final boolean SEQUENTIAL = false;
	private static final boolean USE_LOG = false;

	private static final String CSV_FILE = PathConstants.OUTPUT_DIR + "train_dA_c.csv";
	private static final String OUTPUT_FOLDER = PathConstants.CSV_DIR + "test" + PathConstants.SEP;
	private static final String DELIMITER = ",";
	private static final String ENCODING = "utf-8";
	private static final int WINDOW = 15;
	private static final int ETA = 1000;
	private static final int INPUTS = 48;

	/**
	 * Minimal remaining distance from the estimated beat position to the right
	 * chord boundary. If the distance is less than DELTA, corresponding
	 * spectrogram column will not be included into training data. Therefore
	 * the training data becomes more accurate.
	 */
	private static final double DELTA = 0.5;
	
	/**
	 * Maximum affordable difference between the number of major and minor
	 * chords (but rotation makes it 12 times greater).
	 */
	private static final int L = 100;

	public static void main(String[] args) {
		generateTrainFile();
		generateTestFiles();
	}

	private static void generateTrainFile() {
		LOG.info("Generating train file");
		List<String> tracklist = TracklistCreator.readTrackList(TRAIN_FILE_LIST);
		deleteIfExists(CSV_FILE);
		int filesProcessed = 0;

		for (final String binFileName : tracklist) {
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] result = prepareSpectrum(sd);
			Chord[] chords = prepareChords(binFileName, sd, DELTA);
			processTrainFile(result, chords, 12, CSV_FILE);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info(tracklist.size() + " files were processed. Result was saved to " + CSV_FILE);
	}

	private static void generateTestFiles() {
		LOG.info("Geneating test files");
		List<String> tracklist = TracklistCreator.readTrackList(TEST_FILE_LIST);
		deleteIfExists(OUTPUT_FOLDER);
		int filesProcessed = 0;
		
		for (final String binFileName : tracklist) {
			String csvFileName = OUTPUT_FOLDER + new File(binFileName).getName() + PathConstants.EXT_CSV;
			deleteIfExists(csvFileName);
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] result = prepareSpectrum(sd);
			processTestFile(result, getOutputVectorLength(), csvFileName);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed. Resulting files were saved to " + OUTPUT_FOLDER);

	}

	/**
	 * Delete file or clean directory
	 * @param fileName File or directory
	 */
	private static void deleteIfExists(String fileName) {
		File resultFile = new File(fileName);
		if (resultFile.exists()) {
			if (resultFile.isDirectory()) {
				try {
					FileUtils.cleanDirectory(new File(fileName));
				} catch (IOException e) {
					LOG.error("Could not clean " + fileName, e);
				}
			} else {
				try {
					FileUtils.forceDelete(resultFile);
				} catch (IOException e) {
					LOG.error("Error when deleting file " + fileName, e);
				}
			}
		}
	}

	private static int getOutputVectorLength() {
		return INPUTS + (EXTRA_OCTAVE ? 12 : 0);
	}

	private static void processTrainFile(double[][] data, Chord[] chords, int notesInOctave, String file) {
		if (data == null || chords == null) {
			LOG.error("data or chords is null");
			return;
		}
		if (SEQUENTIAL) {
			for (int offset = 0; offset < 12; offset++) {
				try (OutputStream chordOut = FileUtils.openOutputStream(new File(file), true)) {
					for (int i = 0; i < data.length; i++) {
						int desiredLength = getOutputVectorLength();
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
			int majors = 0;
			int minors = 0;
			for (int i = 0; i < data.length; i++) {
				Chord chord = chords[i];
				if (chord == null) {
					continue;
				}
				boolean containsMajor = containsMajor(chord);
				boolean containsMinor = containsMinor(chord);
				if (containsMajor || containsMinor) {
					if (containsMajor && containsMinor) {
						// do nothing
					} else {
						if (containsMajor && majors - minors > L) {
							continue;
						} else if (containsMajor) {
							majors++;
						} else if (containsMinor && minors - majors > L) {
							continue;
						} else if (containsMinor) {
							minors++;
						}
					}
				}
				try (OutputStream chordOut = FileUtils.openOutputStream(new File(file), true)) {
					for (int offset = 0; offset < 12; offset++) {
						int desiredLength = getOutputVectorLength();
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

	private static void processColumn(double[] data, Chord chord, int offset, OutputStream chordOut) throws IOException {
		if (chord == null) {
			return;
		}
		int from = offset;
		double[] dataLocal = EXTRA_OCTAVE ? Arrays.copyOfRange(data, from, from + INPUTS)
				: rotateArray(data, offset);
		DataUtil.scaleTo01(dataLocal);
		if (chord.isEmpty()) {
			chordOut.write(toByteArray(dataLocal, chord));
		} else if (chord.getNotes().length > 2) {//if (chord.containsTriad(Chord.major(chord.getRoot())) || chord.containsTriad(Chord.minor(chord.getRoot()))) {
			Chord chordLocal = rotateChord(chord, offset);
			chordOut.write(toByteArray(dataLocal, chordLocal));
		}

	}

	private static void processTestFile(double[][] data, int components, String targetFile) {
		if (data == null) {
			LOG.error("data is null");
			return;
		}
		try (OutputStream csvOut = FileUtils.openOutputStream(new File(targetFile), false)) {
			for (int i = 0; i < data.length; i++) {
				double[] col = data[i];
				if (components != col.length) {
					col = Arrays.copyOfRange(col, 0, components);
				}
				DataUtil.scaleTo01(col);
				csvOut.write(toByteArray(col, null));
			}
		} catch (IOException e) {
			LOG.error("Error when writing result", e);
		}
	}

	private static Chord rotateChord(Chord chord, int offset) {
		Note[] notes = chord.getNotes();
		Note[] newNotes = new Note[notes.length];
		for (int j = 0; j < notes.length; j++) {
			newNotes[j] = notes[j].withOffset(-offset);
		}
		Chord chordLocal = new Chord(newNotes);
		return chordLocal;
	}

	private static double[] rotateArray(double[] data, int offset) {
		double[] a1 = ArrayUtils.subarray(data, offset, data.length);
		double[] a2 = ArrayUtils.subarray(data, 0, offset);
		return ArrayUtils.addAll(a1, a2);
	}

	private static double[][] prepareSpectrum(final SpectrumData sd) {
		double[][] result = sd.spectrum;
		result = DataUtil.smoothHorizontallyMedianAndShrink(result, WINDOW, sd.framesPerBeat);
		if (USE_LOG) {
			result = DataUtil.toLogSpectrum(result, ETA);
		}
		result = DataUtil.reduce(result, sd.scaleInfo.octaves);
		DataUtil.scaleEachTo01(result);
		return result;
	}

	private static Chord[] prepareChords(final String binFileName, final SpectrumData sd, double delta) {
		String track = StringUtils.substringAfterLast(binFileName, PathConstants.SEP);
		String labFileName = PathConstants.LAB_DIR + track.replace(PathConstants.EXT_WAV + PathConstants.EXT_BIN, PathConstants.EXT_LAB);
		LabFileReader labReader = new LabFileReader(new File(labFileName));
		Chord[] result = new Chord[sd.beatTimes.length - 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = labReader.getChord(sd.beatTimes[i], delta);
		}
		return result;
	}

	private static byte[] toByteArray(double[] ds, Chord chord) throws UnsupportedEncodingException {
		if (ds == null || ds.length == 0) {
			return new byte[0];
		}
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < ds.length; j++) {
			sb.append(ds[j]);
			sb.append(DELIMITER);
		}
		if (chord != null) {
			if (chord.isEmpty()) {
				sb.append('N');
			} else {
				Note[] notes = chord.getNotes();
				String[] labels = new String[notes.length];
				for (int i = 0; i < notes.length; i++) {
					labels[i] = notes[i].getShortName();
				}
				Arrays.sort(labels, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						return o1.compareTo(o2);
					}  });
				sb.append(StringUtils.join(labels, '-'));
			}
		}
		sb.append("\r\n");
		return sb.toString().getBytes(ENCODING);
	}

	private static boolean containsMajor(Chord chord) {
		if (chord.isMajor()) {
			return true;
		}
		Note[] notes = chord.getNotes();
		for (Note note : notes) {
			if (chord.containsTriad(Chord.major(note))) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsMinor(Chord chord) {
		if (chord.isMinor()) {
			return true;
		}
		Note[] notes = chord.getNotes();
		for (Note note : notes) {
			if (chord.containsTriad(Chord.minor(note))) {
				return true;
			}
		}
		return false;
	}

}
