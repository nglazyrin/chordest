package chordest.io.lab;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.lab.chordparser.ChordParser;
import chordest.io.lab.chordparser.ParseException;
import chordest.io.lab.chordparser.TokenMgrError;
import chordest.model.Chord;



public class LabFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(LabFileReader.class);
	
	private final Chord[] chords;
	private final double[] timestamps;
	
	public LabFileReader(File lab) {
		List<Chord> chordsTemp = new LinkedList<Chord>();
		List<Double> timestampsTemp = new LinkedList<Double>();
		Scanner scanner = null;
		try {
			scanner = new Scanner(lab);
			scanner.useLocale(Locale.ENGLISH);
			double start = 0, end = 0;
			while (scanner.hasNext()) {
				start = scanner.nextDouble();
				end = scanner.nextDouble();
				String chordString = scanner.next();
				Chord chord = ChordParser.parseString(chordString);
				chordsTemp.add(chord);
				timestampsTemp.add(start);
			}
			timestampsTemp.add(end);
//			chordsTemp.add(Chord.empty());
			chords = chordsTemp.toArray(new Chord[chordsTemp.size()]);
			timestamps = new double[timestampsTemp.size()];
			for (int i = 0; i < timestampsTemp.size(); i++) {
				timestamps[i] = timestampsTemp.get(i);
			}
			LOG.info("Chords have been read from " + lab.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (InputMismatchException e) {
			throw new IllegalArgumentException(e);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		} catch (TokenMgrError e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	public Chord[] getChords() {
		return chords;
	}

	public double[] getTimestamps() {
		return timestamps;
	}

	public Chord getChord(double timestamp, double delta) {
		if (timestamp < timestamps[0] || timestamp >= timestamps[timestamps.length - 1]) {
			return Chord.empty();
		}
		int index = Arrays.binarySearch(timestamps, timestamp);
		if (index >= 0) {
			return chords[index];
		} else {
			int insertionPoint = -index - 1;
			if (timestamps[insertionPoint] - timestamp < delta) {
				return null; // doubtful
			}
			return chords[insertionPoint - 1];
		}
	}

}
