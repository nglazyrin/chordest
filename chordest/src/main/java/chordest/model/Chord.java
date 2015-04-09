package chordest.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Models chord as an ordered set of notes. Knows all Harte's chord labels and
 * provides some specific methods.
 * @author Nikolay
 *
 */
public class Chord {

	public static final String MAJ = "maj";
	public static final String MIN = "min";
	public static final String DIM = "dim";
	public static final String AUG = "aug";
	public static final String MAJ7 = "maj7";
	public static final String MIN7 = "min7";
	public static final String DOM = "7";
	public static final String DIM7 = "dim7";
	public static final String HDIM7 = "hdim7";
	public static final String MINMAJ7 = "minmaj7";
	public static final String MAJ6 = "maj6";
	public static final String MIN6 = "min6";
	public static final String NON =  "9";
	public static final String MAJ9 = "maj9";
	public static final String MIN9 = "min9";
	public static final String SUS2 = "sus2";
	public static final String SUS4 = "sus4";
	public static final String N =  "N";
	public static final String NO_SHORTHAND = "";

	public static final String[] START_WITH_MAJ_OR_MIN_OR_N = new String[] {
		MAJ, MAJ7, DOM, MAJ6, NON, MAJ9, MIN, MIN7, MINMAJ7, MIN6, MIN9, N
	};

	private static final Map<String, int[]> shorthandToIntervals = new HashMap<String, int[]>();

	static {
		shorthandToIntervals.put(MAJ, new int[] { Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH });
		shorthandToIntervals.put(MIN, new int[] { Interval.MINOR_THIRD, Interval.PERFECT_FIFTH });
		shorthandToIntervals.put(DIM, new int[] { Interval.MINOR_THIRD, Interval.DIMINISHED_FIFTH });
		shorthandToIntervals.put(AUG, new int[] { Interval.MAJOR_THIRD, Interval.AUGMENTED_FIFTH });
		shorthandToIntervals.put(SUS2, new int[] { Interval.MAJOR_SECOND, Interval.PERFECT_FIFTH });
		shorthandToIntervals.put(SUS4, new int[] { Interval.PERFECT_FOURTH, Interval.PERFECT_FIFTH });
		shorthandToIntervals.put(MAJ7, new int[] { Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MAJOR_SEVENTH });
		shorthandToIntervals.put(MIN7, new int[] { Interval.MINOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH });
		shorthandToIntervals.put(DOM, new int[] { Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH });
		shorthandToIntervals.put(DIM7, new int[] { Interval.MINOR_THIRD, Interval.DIMINISHED_FIFTH, Interval.DIMINISHED_SEVENTH });
		shorthandToIntervals.put(HDIM7, new int[] { Interval.MINOR_THIRD, Interval.DIMINISHED_FIFTH, Interval.MAJOR_SEVENTH });
		shorthandToIntervals.put(MINMAJ7, new int[] { Interval.MINOR_THIRD, Interval.PERFECT_FIFTH, Interval.MAJOR_SEVENTH });
		shorthandToIntervals.put(MAJ6, new int[] { Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH });
		shorthandToIntervals.put(MIN6, new int[] { Interval.MINOR_THIRD, Interval.PERFECT_FIFTH, Interval.MAJOR_SIXTH });
		shorthandToIntervals.put(NON, new int[] { Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH, Interval.NINTH });
		shorthandToIntervals.put(MAJ9, new int[] { Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH, Interval.MAJOR_SEVENTH, Interval.NINTH });
		shorthandToIntervals.put(MIN9, new int[] { Interval.MINOR_THIRD, Interval.PERFECT_FIFTH, Interval.MINOR_SEVENTH, Interval.NINTH });
	}

	//private Map<Integer, Note> components = new HashMap<Integer, Note>();
	private final Note[] components;
	private final String shorthand;
	private final Note bass;

	public static Chord major(Note tonic) {
		return new Chord(tonic, MAJ);
	}

	public static Chord minor(Note tonic) {
		return new Chord(tonic, MIN);
	}

	public static Chord empty() {
		return new Chord();
	}

	public static double[] toPositionArray(Chord c) {
		double[] result = new double[12];
		if (c != null && !c.isEmpty()) {
			for (Note note : c.components) {
				result[note.ordinal()] = 1;
			}
		}
		return result;
	}

	public static Chord fromPositionArray(double[] array) {
		if (array == null || array.length != 12) {
			throw new IllegalArgumentException("array of length 12 expected");
		}
		List<Note> notes = new ArrayList<Note>();
		for (int i = 0; i < 12; i++) {
			if (array[i] > 0) {
				notes.add(Note.byNumber(i));
			}
		}
		return new Chord(notes.toArray(new Note[notes.size()]));
	}

	public static List<Chord> getAllChordsWithShorthands(String[] shorthands) {
		List<Chord> chords = new ArrayList<Chord>(shorthands.length * Note.values().length);
		for (String shorthand : shorthands) {
			for (Note note : Note.values()) {
				chords.add(new Chord(note, shorthand));
			}
		}
		return Collections.unmodifiableList(chords);
	}

	private Chord() {
		this.shorthand = N;
		this.components = new Note[0];
		this.bass = null;
	}

	public Chord(Note... notes) {
		this(0, notes);
	}

	public Chord(int bassInterval, Note... notes) {
		if (notes.length > 0) {
			// first try to build a chord with a known shorthand from the given set of notes
			int[][] permutations = getAllPermutations(notes.length);
			for (int[] permutation : permutations) {
				Note[] tempNotes = new Note[notes.length];
				for (int i = 0; i < permutation.length; i++) {
					tempNotes[i] = notes[permutation[i]];
				}
				String shorthand = tryShorthands(tempNotes);
				if (shorthand != null) {
					this.shorthand = shorthand;
					this.components = tempNotes;
					this.bass = this.components[0].withOffset(bassInterval);
					return;
				}
			}
			// if falied, just copy the notes and don't assign any shorthand
			int i = 0;
			while (i < notes.length && notes[i] != null) { i++; }
			this.shorthand = NO_SHORTHAND;
			this.bass = notes[0].withOffset(bassInterval);
			notes = Arrays.copyOf(notes, i);
			if (! ArrayUtils.contains(notes, this.bass)) {
				notes = ArrayUtils.add(notes, this.bass);
			}
			this.components = notes;
		} else {
			this.shorthand = NO_SHORTHAND;
			this.components = new Note[0];
			this.bass = null;
		}
	}

	public Chord(Note root, String shortHand) {
		this(0, root, shortHand);
	}

	public Chord(int bassInterval, Note root, String shortHand) {
		this.shorthand = String.copyValueOf(shortHand.toCharArray());
		if (N.equals(shortHand)) {
			this.components = new Note[0];
			this.bass = null;
			// do nothing
		} else if (shorthandToIntervals.containsKey(shortHand)) {
			int[] intervals = shorthandToIntervals.get(shortHand);
			this.bass = root.withOffset(bassInterval);
			List<Note> notes = new ArrayList<Note>();
			notes.add(root);
			for (int interval : intervals) {
				notes.add(root.withOffset(interval));
			}
//			if (! notes.contains(this.bass)) {
//				notes.add(this.bass);
//			}
			this.components = notes.toArray(new Note[notes.size()]);
		} else {
			if (root != null) {
				this.bass = root.withOffset(bassInterval);
				if (root.equals(bass)) {
					this.components = new Note[] { root };
				} else {
					this.components = new Note[] { root, bass };
				}
			} else {
				this.components = new Note[0];
				this.bass = null;
			}
		}
	}

	private String tryShorthands(Note[] notes) {
		if (notes == null) {
			return null;
		}
		for (String shorthand : shorthandToIntervals.keySet()) {
			int[] intervals = shorthandToIntervals.get(shorthand);
			if (notes.length == intervals.length + 1 && matchesIntervals(notes, intervals)) {
				return shorthand;
			}
		}
		return null;
	}

	private Boolean matchesIntervals(Note[] notes, int[] intervals) {
		Note root = notes[0];
		for (int i = 0; i < intervals.length; i++) {
			if (((notes[i+1].ordinal() - root.ordinal() + 12) % 12) != intervals[i]) {
				return false;
			}
		}
		return true;
	}

	// see http://stackoverflow.com/questions/4240080/generating-all-permutations-of-a-given-string
	private int[][] getAllPermutations(int k) {
		List<int[]> result = new ArrayList<int[]>();
		int[] array = new int[k];
		for (int i = 0; i < k; i++) { array[i] = i; }
		getPermutations(result, k, new int[0], array);
		return result.toArray(new int[result.size()][]);
	}

	private void getPermutations(List<int[]> result, int k, int[] prefix, int[] array) {
		if (array.length == 0) {
			result.add(prefix);
			return;
		} else {
			for (int i = 0; i < array.length; i++) {
				getPermutations(result, k, ArrayUtils.add(prefix, array[i]),
						ArrayUtils.addAll(ArrayUtils.subarray(array, 0, i), ArrayUtils.subarray(array, i+1, array.length)));
			}
		}
	}

	/**
	 * @return Modifiable list of notes
	 */
	public List<Note> getNotesAsList() {
		List<Note> result = new ArrayList<Note>();
		for (Note note : this.components) { result.add(note); }
		return result;
	}

	public Note[] getNotes() {
		return this.components;
	}

	public Note getRoot() {
		return getNote(0);
	}

	private Note getNote(int position) {
		if (this.components == null || position < 0 || position >= this.components.length) {
			return null;
		}
		return this.components[position];
	}

	public String getShortHand() {
		return shorthand;
	}

	public Note getBass() {
		return bass;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) { return false; }
		if (o == this) { return true; }
		if (!(o instanceof Chord)) { return false; }
		Chord other = (Chord)o;
		if (this.isShortHandDefined() && other.isShortHandDefined()) {
			if (Chord.AUG.equals(this.shorthand) && Chord.AUG.equals(other.shorthand)) {
				return hasSameNotesWith(other);
			} else {
				return (this.getRoot() == other.getRoot()) &&
					(this.shorthand.equals(other.shorthand));
//				|| (MAJ.equals(this.shorthand) && MAJ7.equals(other.shorthand))
//				|| (MAJ7.equals(this.shorthand) && MAJ.equals(other.shorthand))
//				|| (MIN.equals(this.shorthand) && MIN7.equals(other.shorthand))
//				|| (MIN7.equals(this.shorthand) && MIN.equals(other.shorthand)));
			}
		} else {
			return hasSameNotesWith(other);
		}
	}

	public boolean hasSameBassWith(Chord other) {
		if (bass == null && other.bass == null) {
			return true;
		}
		if (bass != null && bass.equals(other.bass)) {
			return true;
		}
		return false;
	}

	private boolean hasSameNotesWith(Chord other) {
		if (this.getNotes().length != other.getNotes().length) { return false; }
		Set<Note> notes = new LinkedHashSet<Note>();
		notes.addAll(this.getNotesAsList());
		if (notes.containsAll(other.getNotesAsList())) {
			return true; // because their sizes are already equal
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = 0;
		int i = 2;
		for (Note note : getNotes()) {
			result += i * note.hashCode();
			i++;
		}
		if (this.bass != null) {
			result += bass.hashCode();
		}
		return result;
	}

	public int getNumberOfCommonNotes(Chord other) {
		List<Note> thisNotes = this.getNotesAsList();
		int common = 0;
		for (Note note : other.getNotes()) {
			if (thisNotes.contains(note)) {
				common++;
			}
		}
		return common;
	}

	public boolean containsTriad(Chord triad) {
		if (triad == null || triad.isEmpty() || this.isEmpty()) {
			return equals(triad);
		}
		if (! (triad.isMajor() || triad.isMinor())) {
			throw new IllegalArgumentException("Major or minor expected, but was: " + triad);
		}
		// so further assume that triad is either major or minor
		int common = 0;
		for (Note note : triad.components) {
			if (this.hasNote(note)) {
				common++;
			}
		}
		return common >= 3;
	}

	@Override
	public String toString() {
		if (this.isEmpty()) {
			return "N";
		}
		StringBuilder sb = new StringBuilder();
		if (this.shorthand != null && !("".equals(this.shorthand)) && !("N".equals(this.shorthand))) {
			sb.append(this.getRoot().getShortName());
			if (! MAJ.equals(this.shorthand)) {
				sb.append(":");
				sb.append(this.shorthand);
			}
		} else if (this.isMajor()) {
			sb.append(this.getRoot().getShortName());
			sb.append(":maj");
		} else if (this.isMinor()) {
			sb.append(this.getRoot().getShortName());
			sb.append(":min");
		} else {
			int cumulativeOffset = 0;
			Note previous = this.components[0];
			sb.append(this.getRoot().getShortName());
			sb.append(":(");
			for (int i = 1; i < this.components.length; i++) {
				Note n = this.components[i];
				int offset = (n.offsetFrom(previous) + 12) % 12;
				cumulativeOffset += offset;
				previous = n;
				sb.append(Interval.toInterval(cumulativeOffset));
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")");
		}
		if (this.bass != null && ! this.bass.equals(this.getRoot())) {
			sb.append("/");
			sb.append(Interval.toInterval((bass.offsetFrom(this.getRoot()) + 12) % 12));
		}
		return sb.toString();
	}

	public boolean hasNote(Note note) {
		for (Note current : this.components) {
			if (current.equals(note)) {
				return true;
			}
		}
		return false;
	}

	public boolean isMajor() {
		return isOfType(MAJ);
	}

	public boolean isMinor() {
		return isOfType(MIN);
	}

	public boolean isEmpty() {
		return this.components.length == 0 || N.equals(this.shorthand);
	}

	public boolean isOfType(String shortHand) {
		if (shortHand == null) {
			return false;
		}
		return shortHand.equals(this.shorthand);
	}

	private boolean isShortHandDefined() {
		return ! NO_SHORTHAND.equals(shorthand);
	}

	public boolean hasSameRootDifferentType(Chord other) {
		if (isEmpty() || other.isEmpty()) {
			return false;
		}
		return this.getRoot().equals(other.getRoot()) &&
				! this.getShortHand().equals(other.getShortHand());
	}

}
