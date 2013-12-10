package chordest.model;

import java.util.Arrays;
import java.util.List;

import chordest.model.Scale.NaturalMajor;
import chordest.model.Scale.NaturalMinor;

/**
 * Major and minor keys + a method to detect key using Krumhansl's profiles.
 * @author Nikolay
 *
 */
public class Key {

//	private static final int[] majorOffsets = { 2, 2, 1, 2, 2, 2, 1 };
//	private static final int[] minorOffsets = { 2, 1, 2, 2, 1, 2, 2 };

	private final Note root;

	private final Scale type;

	public Key(Note root, Scale type) {
		this.root = root;
		this.type = type;
	}

	public Note getRoot() {
		return root;
	}

	public Scale getType() {
		return type;
	}

	public List<Note> getNotes() {
		Note[] result = new Note[type.intervals.length + 1];
		result[0] = root;
		for (int i = 0; i < type.intervals.length; i++) {
			result[i + 1] = root.withOffset(type.intervals[i]);
		}
		return Arrays.asList(result);
	}

	public List<Chord> getChords() {
		Chord[] result = type.getChords(root);
		return Arrays.asList(result);
	}

	public Key getSubdominant() {
		return new Key(root.withOffset(Interval.PERFECT_FOURTH), type);
	}

	public Key getDominant() {
		return new Key(root.withOffset(Interval.PERFECT_FIFTH), type);
	}

	/**
	 * Tries to recognize key using Krumhansl's key-finding algorithm
	 * @param intensities 12-dimensional array, first component corresponds to A
	 * @return
	 */
	public static Key recognizeKey(double[] intensities, Note startNote) {
		if (intensities == null) {
			throw new NullPointerException("durations is null");
		}
		if (intensities.length != 12) {
			throw new IllegalArgumentException("durations.length != 12");
		}
//		double[] template = new double[] { 6.35, 2.23, 3.48, 2.33, 4.38, 4.09,
//				2.52, 5.19, 2.39, 3.66, 2.29, 2.88};
		double[] majorTemperley = new double[] { 5, 2, 3.5, 2, 4.5, 4, 2, 4.5, 2, 3.5, 1.5, 4 };
		double[] minorTemperley = new double[] { 5, 2, 3.5, 4.5, 2, 4, 2, 4.5, 3.5, 2, 1.5, 4 };
		double[] correlations = new double[24];
		for (int i = 0; i < 12; i++) {
			for (int j = 0; j < 12; j++) {
				correlations[i] += intensities[j] * majorTemperley[j];
				correlations[i + 12] += intensities[j] * minorTemperley[j];
			}
			majorTemperley = rotateLeft(majorTemperley);
			minorTemperley = rotateLeft(minorTemperley);
		}
		
		int maxPos = 0; double max = correlations[0];
		for (int i = 1; i < correlations.length; i++) {
			if (correlations[i] > max) {
				maxPos = i; max = correlations[i];
			}
		}
		return new Key(startNote.withOffset(maxPos % 12), maxPos < 12 ? new NaturalMajor() : new NaturalMinor());
	}

	private static double[] rotateLeft(double[] array) {
		double[] result = new double[array.length];
		for (int k = array.length - 1; k > 0; k--) { result[k] = array[k - 1]; }
		result[0] = array[array.length - 1];
		return result;
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
