package chordest.chord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Key {

	private static final Logger LOG = LoggerFactory.getLogger(Key.class);

	private static final int[] majorOffsets = { 2, 2, 1, 2, 2, 2, 1 };
	private static final int[] minorOffsets = { 2, 1, 2, 2, 1, 2, 2 };

	public static final Key A_MAJ = new Key(Note.A, Chord.MAJ);
	public static final Key AD_MAJ = new Key(Note.AD, Chord.MAJ);
	public static final Key B_MAJ = new Key(Note.B, Chord.MAJ);
	public static final Key C_MAJ = new Key(Note.C, Chord.MAJ);
	public static final Key CD_MAJ = new Key(Note.CD, Chord.MAJ);
	public static final Key D_MAJ = new Key(Note.D, Chord.MAJ);
	public static final Key DD_MAJ = new Key(Note.DD, Chord.MAJ);
	public static final Key E_MAJ = new Key(Note.E, Chord.MAJ);
	public static final Key F_MAJ = new Key(Note.F, Chord.MAJ);
	public static final Key FD_MAJ = new Key(Note.FD, Chord.MAJ);
	public static final Key G_MAJ = new Key(Note.G, Chord.MAJ);
	public static final Key GD_MAJ = new Key(Note.GD, Chord.MAJ);

	public static final Key[] MAJORS = new Key[] { A_MAJ, AD_MAJ, B_MAJ,
		C_MAJ, CD_MAJ, D_MAJ, DD_MAJ, E_MAJ, F_MAJ, FD_MAJ, G_MAJ, GD_MAJ};

	private Note root;

	private String type;

	public Key(Note root, String type) {
		this.root = root;
		this.type = type;
	}

	public Note getRoot() {
		return root;
	}

	public String getType() {
		return type;
	}

	public List<Note> getNotes() {
		List<Note> result = new ArrayList<Note>(7);
		Note current = getRoot();
		if (Chord.MIN.equals(type)) {
			for (int i = 0; i < 6; i++) {
				result.add(current);
				current = current.withOffset(minorOffsets[i]);
			}
		} else {
			for (int i = 0; i < 6; i++) {
				result.add(current);
				current = current.withOffset(majorOffsets[i]);
			}
		}
		result.add(current); // add latest, 7th, note
		return Collections.unmodifiableList(result);
	}

	public List<Chord> getChords() {
		List<Note> notes = getNotes();
		List<Chord> result = new ArrayList<Chord>(6);
		result.add(Chord.major(notes.get(0)));
		result.add(Chord.minor(notes.get(1)));
		result.add(Chord.minor(notes.get(2)));
		result.add(Chord.major(notes.get(3)));
		result.add(Chord.major(notes.get(4)));
		result.add(Chord.minor(notes.get(5)));
		
//		result.add(new Chord(notes.get(0), Chord.MAJ7));
//		result.add(new Chord(notes.get(1), Chord.MIN7));
//		result.add(new Chord(notes.get(2), Chord.MIN7));
//		result.add(new Chord(notes.get(3), Chord.MAJ7));
//		result.add(new Chord(notes.get(4), Chord.DOM));
//		result.add(new Chord(notes.get(5), Chord.MIN7));
		return Collections.unmodifiableList(result);
	}

	/**
	 * Tries to recognize key using Krumhansl's key-finding algorithm
	 * @param durations 12-dimensional array, first component corresponds to A
	 * @return
	 */
	public static Key recognizeKey(double[] durations, Note startNote) {
		if (durations == null) {
			throw new NullPointerException("durations is null");
		}
		if (durations.length != 12) {
			throw new IllegalArgumentException("durations.length != 12");
		}
		double[] template = new double[] { 6.35, 2.23, 3.48, 2.33, 4.38, 4.09,
				2.52, 5.19, 2.39, 3.66, 2.29, 2.88};
		double[] correlations = new double[12];
		for (int i = 0; i < 12; i++) {
			for (int j = 0; j < 12; j++) {
				correlations[i] += durations[j] * template[j];
			}
			double temp = template[11];
			for (int k = 11; k > 0; k--) { template[k] = template[k - 1]; }
			template[0] = temp;
		}
		
		int maxPos = 0; double max = correlations[0];
		for (int i = 1; i < 12; i++) {
			if (correlations[i] > max) {
				maxPos = i; max = correlations[i];
			}
		}
		return new Key(startNote.withOffset(maxPos), Chord.MAJ);
	}

	@Override
	public String toString() {
		return root.getShortName() + ":" + type;
	}

	@Override
	public boolean equals(Object other) {
		if (! (other instanceof Key)) { return false; }
		if (other == this) { return true; }
		Key otherMode = (Key)other;
		return this.root.equals(otherMode.root) && this.type.equals(otherMode.type);
	}

	@Override
	public int hashCode() {
		int p = 31;
		return root.hashCode() * p + type.hashCode();
	}

}
