package chordest.chord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chordest.model.Chord;
import chordest.model.Key;
import chordest.model.Note;
import chordest.model.Scale.NaturalMajor;
import chordest.model.Scale.NaturalMinor;
import chordest.util.DataUtil;

public class KeyExtractor {

	private static final Set<Key> possibleKeys = new HashSet<Key>();

	static {
		for (Note note : Note.values()) {
			possibleKeys.add(new Key(note, new NaturalMajor()));
			possibleKeys.add(new Key(note, new NaturalMinor()));
		}
	}

	public static Key getKey(double[][] chroma, Note startNote) {
		if (chroma[0].length > 12) {
			chroma = DataUtil.reduceTo12Notes(chroma);
		} else if (chroma[0].length < 12) {
			throw new IllegalArgumentException("chroma must be 12-dimensional, but was " + chroma[0].length);
		}
		double[] sum = new double[12];
		for (int i = 0; i < chroma.length; i++) {
			sum = DataUtil.add(sum, chroma[i]);
		}
		return Key.recognizeKey(sum, startNote);
	}

	public static Key getKey(Chord[] chords, Note startNote) {
		double[] sum = new double[12];
		for (Chord chord : chords) {
			double[] temp = new double[12];
			for (Note note : chord.getNotes()) {
				temp[(note.ordinal() - startNote.ordinal() + 12) % 12] = 1;
			}
			sum = DataUtil.add(sum, temp);
		}
		return Key.recognizeKey(sum, startNote);
	}

	public static List<Key> getPossibleKeys(Collection<Chord> chords) {
		List<Key> result = new ArrayList<Key>();
		for (Key key : possibleKeys) {
			boolean hasAllChords = true;
			for (Chord chord : chords) {
//				if (! key.hasChord(chord)) {
//					hasAllChords = false;
//				}
			}
			if (hasAllChords) {
				result.add(key);
			}
		}
		return result;
	}

}
