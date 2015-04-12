package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
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

	public static final int PARTS = 2;
	private static final boolean EXTRA_OCTAVE = true;
	private static final boolean SEQUENTIAL = true;
	private static final boolean USE_LOG = true; 
	private static final int WINDOW = 15;
	private static final int ETA = 50000;
	public static final int INPUTS = 48;
	private static final double THETA = 0.08;

	public static final String MODEL_FOLDER = "E:\\dev\\git\\my_repository\\chordest\\work\\";
	public static final String ROOT_FOLDER = "E:\\dev\\spectra\\csv\\";
	private static final String NUM = INPUTS == 60 ? "60" : "";
	private static final String SUFFIX = USE_LOG ? "" : ".nolog";
	private static final String DELIMITER = ",";
	private static final String ENCODING = "utf-8";

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
		for (int index = 0; index < PARTS; index++) {
			generateTrainFile(index);
			generateTestFiles(index);
		}
	}

	public static String getTrainFileListName(int index) {
		return PREFIX + "bqrz_bin" + index + "train.txt";
	}

	public static String getTestFileListName(int index) {
		return PREFIX + "bqrz_bin" + index + "test.txt";
	}

	private static String getCsvFileName(int index) {
		return ROOT_FOLDER + "train\\train" + NUM + index + SUFFIX + ".csv";
	}

	private static String getOutputFolderName(int index) {
		return ROOT_FOLDER + "test" + NUM + index + SUFFIX + "\\";
	}

	public static void iterateTrainFiles(int index, Consumer< Pair<double[][], Chord[]> > consumer) {
		List<String> tracklist = TracklistCreator.readTrackList(getTrainFileListName(index));
		int filesProcessed = 0;
		for (final String binFileName : tracklist) {
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] spectrum = prepareSpectrum(sd);
			Chord[] chords = prepareChords(binFileName, sd, DELTA);
			consumer.accept(new Pair<double[][], Chord[]>(spectrum, chords));
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info(tracklist.size() + " files have been processed.");
	}

	public static void iterateTestFiles(int index, Consumer< Pair<double[][], String> > consumer) {
		List<String> tracklist = TracklistCreator.readTrackList(getTrainFileListName(index));
		int filesProcessed = 0;
		for (final String binFileName : tracklist) {
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] spectrum = prepareSpectrum(sd);
			consumer.accept(new Pair<double[][], String>(spectrum, binFileName));
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info(tracklist.size() + " files have been processed.");
	}

	private static void generateTrainFile(int index) {
		LOG.info("Generating train file");
		String csvFileName = getCsvFileName(index);
		deleteIfExists(csvFileName);
		iterateTrainFiles(index, pair -> processTrainFile(pair.getFirst(), pair.getSecond(), 12, csvFileName));
		LOG.info("Result was saved to " + csvFileName);
	}

	private static void generateTestFiles(int index) {
		LOG.info("Geneating test files");
		String outputFolder = getOutputFolderName(index);
		List<String> tracklist = TracklistCreator.readTrackList(getTestFileListName(index));
		deleteIfExists(outputFolder);
		int filesProcessed = 0;
		
		for (final String binFileName : tracklist) {
			String csvFileName = outputFolder + new File(binFileName).getName() + PathConstants.EXT_CSV;
			deleteIfExists(csvFileName);
			SpectrumData sd = SpectrumFileReader.read(binFileName);
			double[][] result = prepareSpectrum(sd);
			processTestFile(result, getOutputVectorLength(true), csvFileName);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed. Resulting files were saved to " + outputFolder);

	}

	/**
	 * Delete file or clean directory
	 * @param fileName File or directory
	 */
	public static void deleteIfExists(String fileName) {
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

	public static int getOutputVectorLength(boolean forTest) {
		if (forTest) {
			return INPUTS + (EXTRA_OCTAVE ? 12 : 0);
		} else {
			return INPUTS;
		}
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
						int desiredLength = getOutputVectorLength(false);
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
						int desiredLength = getOutputVectorLength(false);
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

	private static void processColumn(double[] data, Chord chord, int offset, OutputStream chordOut) {
		Pair<double[], Chord> p = rotateSingleColumn(data, chord, offset);
		try {
			chordOut.write(toByteArray(p.getFirst(), p.getSecond()));
		} catch (IOException e) {
			LOG.error("Error when writing result", e);
		}
	}

	public static Pair<double[], Chord> rotateSingleColumn(double[] data, Chord chord, int offset) {
		if (chord == null) {
			return new Pair<double[], Chord>(new double[INPUTS], chord);
		}
		double[] dataLocal = EXTRA_OCTAVE ? Arrays.copyOfRange(data, offset, offset + INPUTS)
				: rotateArray(data, offset);
		DataUtil.scaleTo01(dataLocal);
		Chord chordLocal = rotateChord(chord, offset);
		return new Pair<double[], Chord>(dataLocal, chordLocal);
//		if (chord.isEmpty()) {
//			consumer.accept(new Pair<double[], Chord>(dataLocal, chord));
//		} else if (chord.getNotes().length > 2) {//if (chord.containsTriad(Chord.major(chord.getRoot())) || chord.containsTriad(Chord.minor(chord.getRoot()))) {
//			Chord chordLocal = rotateChord(chord, offset);
//			consumer.accept(new Pair<double[], Chord>(dataLocal, chordLocal));
//		}
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
		if (chord.isEmpty()) {
			return chord;
		}
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
		
//		double[][] selfSim = DataUtil.getSelfSimilarity(result);
//		selfSim = DataUtil.removeDissimilar(selfSim, THETA);
//		result = DataUtil.smoothWithSelfSimilarity(result, selfSim);
		
		return result;
	}

	private static Chord[] prepareChords(final String binFileName, final SpectrumData sd, double delta) {
		String track = StringUtils.substringAfterLast(binFileName, PathConstants.SEP);
		String labFileName = PathConstants.LAB_DIR + track.replace(PathConstants.EXT_WAV + PathConstants.EXT_BIN, PathConstants.EXT_LAB);
		LabFileReader labReader = new LabFileReader(new File(labFileName));
		double[] beatTimes = DataUtil.toAllBeatTimes(sd.beatTimes, sd.framesPerBeat);
		Chord[] result = new Chord[beatTimes.length - 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = labReader.getChord(beatTimes[i], delta);
		}
		return result;
	}

	public static byte[] toByteArray(double[] ds, Chord chord) throws UnsupportedEncodingException {
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
