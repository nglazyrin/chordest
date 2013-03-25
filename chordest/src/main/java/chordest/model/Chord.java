package chordest.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

	//private Map<Integer, Note> components = new HashMap<Integer, Note>();
	private final Note[] components;
	private final String shorthand;

	public static Chord major(Note tonic) {
		return new Chord(tonic, MAJ);
	}

	public static Chord minor(Note tonic) {
		return new Chord(tonic, MIN);
	}

	public static Chord empty() {
		return new Chord();
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
	}

	public Chord(Note... notes) {
		Set<Note> set = new HashSet<Note>();
		set.addAll(Arrays.asList(notes));
		set.remove(null);
		Note[] array = set.toArray(new Note[set.size()]);
		if (array.length > 0) {
			Note rootMaj = tryMajor(array);
			Note rootMin = tryMinor(array);
			if (rootMaj != null) {
				this.shorthand = Chord.MAJ;
				this.components = new Note[] { rootMaj, rootMaj.withOffset(Interval.MAJOR_THIRD), rootMaj.withOffset(Interval.PERFECT_FIFTH) };
			} else if (rootMin != null) {
				this.shorthand = Chord.MIN;
				this.components = new Note[] { rootMin, rootMin.withOffset(Interval.MINOR_THIRD), rootMin.withOffset(Interval.PERFECT_FIFTH) };
			} else {
				int i = 0;
				while (i < array.length && array[i] != null) { i++; }
				this.shorthand = NO_SHORTHAND;
				this.components = Arrays.copyOf(array, i);
			}
		} else {
			this.shorthand = NO_SHORTHAND;
			this.components = new Note[0];
		}
	}

	public Chord(Note root, String shortHand) {
		this.shorthand = String.copyValueOf(shortHand.toCharArray());
		if (MAJ.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH) };
		} else if (MIN.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH) };
		} else if (DIM.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.DIMINISHED_FIFTH) };
		} else if (AUG.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_THIRD), root.withOffset(Interval.AUGMENTED_FIFTH) };
		} else if (MAJ7.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MAJOR_SEVENTH) };
		} else if (MIN7.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MINOR_SEVENTH) };
		} else if (DOM.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MINOR_SEVENTH) };
		} else if (DIM7.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.DIMINISHED_FIFTH), root.withOffset(Interval.DIMINISHED_SEVENTH) };
		} else if (HDIM7.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.DIMINISHED_FIFTH), root.withOffset(Interval.MINOR_SEVENTH) };
		} else if (MINMAJ7.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MAJOR_SEVENTH) };
		} else if (MAJ6.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MAJOR_SIXTH) };
		} else if (MIN6.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MAJOR_SIXTH) };
		} else if (NON.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MINOR_SEVENTH), root.withOffset(Interval.NINTH) };
		} else if (MAJ9.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MAJOR_SEVENTH), root.withOffset(Interval.NINTH) };
		} else if (MIN9.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MINOR_THIRD), root.withOffset(Interval.PERFECT_FIFTH), root.withOffset(Interval.MINOR_SEVENTH), root.withOffset(Interval.NINTH) };
		} else if (SUS2.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.MAJOR_SECOND), root.withOffset(Interval.PERFECT_FIFTH) };
		} else if (SUS4.equals(shortHand)) {
			this.components = new Note[] { root, root.withOffset(Interval.PERFECT_FOURTH), root.withOffset(Interval.PERFECT_FIFTH) };
		} else if (N.equals(shortHand)) {
			this.components = new Note[0];
			// do nothing
		} else {
			if (root != null) {
				this.components = new Note[] { root };
			} else {
				this.components = new Note[0];
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
		for (Note note : getNotes()) {
			result += note.hashCode(); 
		}
		return result;
	}

	public boolean hasCommon3Notes(Chord other) {
		List<Note> thisNotes = this.getNotesAsList();
		int common = 0;
		for (Note note : other.getNotes()) {
			if (thisNotes.contains(note)) {
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
			for (Note n : this.components) {
				sb.append(n.getShortName());
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
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
		return MAJ.equals(shorthand);
	}

	public boolean isMinor() {
		return MIN.equals(shorthand);
	}

	private Note tryMajor(Note[] notes) {
		if (notes == null || notes.length != 3) {
			return null;
		}
		if (isMajorWithRootN0(notes[0], notes[1], notes[2]) || isMajorWithRootN0(notes[0], notes[2], notes[1])) {
			return notes[0];
		} else if (isMajorWithRootN0(notes[1], notes[0], notes[2]) || isMajorWithRootN0(notes[1], notes[2], notes[0])) {
			return notes[1];
		} else if (isMajorWithRootN0(notes[2], notes[0], notes[1]) || isMajorWithRootN0(notes[2], notes[1], notes[0])) {
			return notes[2];
		}
		return null;
	}

	private boolean isMajorWithRootN0(Note n0, Note n1, Note n2) {
		return (n1.ordinal() - n0.ordinal() + 12) % 12 == Interval.MAJOR_THIRD &&
				(n2.ordinal() - n0.ordinal() + 12) % 12 == Interval.PERFECT_FIFTH;
	}

	private Note tryMinor(Note[] notes) {
		if (notes == null || notes.length != 3) {
			return null;
		}
		if (isMinorWithRootN0(notes[0], notes[1], notes[2]) || isMinorWithRootN0(notes[0], notes[2], notes[1])) {
			return notes[0];
		} else if (isMinorWithRootN0(notes[1], notes[0], notes[2]) || isMinorWithRootN0(notes[1], notes[2], notes[0])) {
			return notes[1];
		} else if (isMinorWithRootN0(notes[2], notes[0], notes[1]) || isMinorWithRootN0(notes[2], notes[1], notes[0])) {
			return notes[2];
		}
		return null;
	}

	private boolean isMinorWithRootN0(Note n0, Note n1, Note n2) {
		return (n1.ordinal() - n0.ordinal() + 12) % 12 == Interval.MINOR_THIRD &&
				(n2.ordinal() - n0.ordinal() + 12) % 12 == Interval.PERFECT_FIFTH;
	}

	public boolean isEmpty() {
		return this.components.length == 0 || N.equals(this.shorthand);
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
