package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Note;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class HmmDataGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(HmmDataGenerator.class);
	private static final String CHORD_FILE = PathConstants.OUTPUT_DIR + "train_hmm_chord.csv";
	private static final String SEQ_FILE = PathConstants.OUTPUT_DIR + "train_hmm_seq.csv";

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.readTrackList(TrainDataGenerator.TRAIN_FILE_LIST);
		TrainDataGenerator.deleteIfExists(CHORD_FILE);
		TrainDataGenerator.deleteIfExists(SEQ_FILE);
		int filesProcessed = 0;
		for (final String binFileName : tracklist) {
			HmmDataGenerator hdg = new HmmDataGenerator();
			double[][] result = TrainDataGenerator.prepareSpectrum(binFileName);
			Chord[] chords = TrainDataGenerator.prepareChords(binFileName, 0.5);
			hdg.process(result, chords, TrainDataGenerator.OFFSET, TrainDataGenerator.INPUTS);
			if (++filesProcessed % 10 == 0) {
				LOG.info(filesProcessed + " files processed");
			}
		}
		LOG.info("Done. " + tracklist.size() + " files were processed. Result was saved to "
				+ CHORD_FILE + " and " + SEQ_FILE);
	}

	private void process(double[][] data, Chord[] chords, int offset, int components) {
		if (data == null || chords == null) {
			LOG.error("data or chords is null");
			return;
		}
		
		File chordFile = new File(CHORD_FILE);
		File seqFile = new File(SEQ_FILE);
		try (OutputStream chordOut = FileUtils.openOutputStream(chordFile, true);
				OutputStream seqOut = FileUtils.openOutputStream(seqFile, true)) {
			for (int i = 0; i < data.length; i++) {
				double[] row = data[i];
				Chord chordSeq = chords[i];
				if (chordSeq != null) {
//					double[] rowChord = toC(row, chordSeq, offset, components);
					double[] rowChord = Arrays.copyOfRange(row, offset, offset + components + 12);
					double[] rowSeq = Arrays.copyOfRange(row, offset, offset + components + 12);
					if (chordSeq.isEmpty() || chordSeq.isMajor() || chordSeq.isMinor()) {
//						Chord chordC = toC(chordSeq);
						Chord chordC = chordSeq;
						chordOut.write(toByteArray(rowChord, chordC));
						seqOut.write(toByteArray(rowSeq, chordSeq));
					}
				}
			}
		} catch (IOException e) {
			LOG.error("Error when writing result", e);
		}
	}

	/**
	 * Spectrum must contain 12 elements per octave
	 * @param spectrum
	 * @param chord
	 * @return
	 */
	private double[] toC(double[] spectrum, Chord chord, int offset, int components) {
		if (chord.isEmpty()) {
			return Arrays.copyOfRange(spectrum, offset, offset + components);
		} else if (chord.isMajor() || chord.isMinor()) {
			int offsetFromC = chord.getRoot().offsetFrom(Note.C);
			int from = offset + offsetFromC;
			if (from < 0) { 
				from += 12;
			}
			int to = from + components;
			if (to > spectrum.length) {
				throw new RuntimeException("Unable to select spectrum[" + from + "," + to + "] from an array of length " + spectrum.length);
			}
			return Arrays.copyOfRange(spectrum, from, to);
		} else {
			return null;
		}
	}

	private Chord toC(Chord chord) {
		if (chord.isEmpty()) {
			return chord;
		} else if (chord.isMajor()) {
			return Chord.major(Note.C);
		} else if (chord.isMinor()) {
			return Chord.minor(Note.C);
		}
		return null;
	}

	private byte[] toByteArray(double[] ds, Chord chord) throws UnsupportedEncodingException {
		if (ds == null || ds.length == 0) {
			return new byte[0];
		}
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < ds.length; j++) {
			sb.append(ds[j]);
			sb.append(TrainDataGenerator.DELIMITER);
		}
		if (chord != null) {
			sb.append(chord.toString());
		}
		sb.append("\r\n");
		return sb.toString().getBytes(TrainDataGenerator.ENCODING);
	}

}
