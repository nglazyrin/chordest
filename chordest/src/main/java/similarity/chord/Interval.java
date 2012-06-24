package similarity.chord;

public class Interval {

	public final static int TONIC = 0;
	public final static int PERFECT_UNISON = 0;
	public final static int DIMINISHED_SECOND = PERFECT_UNISON;
	public final static int MINOR_SECOND = 1;
	public final static int AUGMENTED_UNISON = MINOR_SECOND;
	public final static int MAJOR_SECOND = 2;
	public final static int DIMINISHED_THIRD = MAJOR_SECOND;
	public final static int MINOR_THIRD = 3;
	public final static int AUGMENTED_SECOND = MINOR_THIRD;
	public final static int MAJOR_THIRD = 4;
	public final static int DIMINISHED_FOURTH = MAJOR_THIRD;
	public final static int PERFECT_FOURTH = 5;
	public final static int AUGMENTED_THIRD = PERFECT_FOURTH;
	public final static int DIMINISHED_FIFTH = 6;
	public final static int AUGMENTED_FOURTH = DIMINISHED_FIFTH;
	public final static int PERFECT_FIFTH = 7;
	public final static int DIMINISHED_SIXTH = PERFECT_FIFTH;
	public final static int MINOR_SIXTH = 8;
	public final static int AUGMENTED_FIFTH = MINOR_SIXTH;
	public final static int MAJOR_SIXTH = 9;
	public final static int DIMINISHED_SEVENTH = MAJOR_SIXTH;
	public final static int MINOR_SEVENTH = 10;
	public final static int AUGMENTED_SIXTH = MINOR_SEVENTH;
	public final static int MAJOR_SEVENTH = 11;
	public final static int DIMINISHED_OCTAVE = MAJOR_SEVENTH;
	public final static int PERFECT_OCTAVE = 12;
	public final static int AUGMENTED_SEVENTH = PERFECT_OCTAVE;
	public final static int NINTH = 14;
	public final static int TENTH = 16;
	public final static int ELEVENTH = 17;

	public static int toOffset(int interval) {
		switch (interval) {
		case 1 : return PERFECT_UNISON;
		case 2 : return MAJOR_SECOND;
		case 3 : return MAJOR_THIRD;
		case 4 : return PERFECT_FOURTH;
		case 5 : return PERFECT_FIFTH;
		case 6 : return MAJOR_SIXTH;
		case 7 : return MAJOR_SEVENTH;
		case 8 : return PERFECT_OCTAVE;
		case 9 : return NINTH;
		case 10 : return TENTH;
		case 11 : return ELEVENTH;
		default : return 0;
		}
	}

}
