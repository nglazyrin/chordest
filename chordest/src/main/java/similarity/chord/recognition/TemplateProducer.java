package similarity.chord.recognition;

import similarity.chord.Chord;
import similarity.chord.Note;


public class TemplateProducer {

	private static final int HARMONICS_COUNT = 3;
	private static final double[] HARMONIC_CONTRIBUTIONS = new double[HARMONICS_COUNT];
	private static final double CONTRIBUTION_REDUCTION = 0;

	static {
		initializeHarmonicContributions();
	}

	private static void initializeHarmonicContributions() {
		for (int i = 0; i < HARMONICS_COUNT; i++) {
			HARMONIC_CONTRIBUTIONS[i] = getIthHarmonicContribution(i);
		}
	}

	private static double getIthHarmonicContribution(int i) {
		return Math.pow(CONTRIBUTION_REDUCTION, i);
	}

	private final Note startNote;

	public TemplateProducer(Note startNote) {
		this.startNote = startNote;
	}

	public double[] getTemplateFor(Chord chord) {
		double[] template = new double[12];
		Note[] notes = chord.getNotes();
		if (chord.isMajor() || chord.isMinor()) {
			fillHarmonicsForPitchClass(template, notes[0], 1.3);	// step 5 1.3
			fillHarmonicsForPitchClass(template, notes[1], 1.0);
			fillHarmonicsForPitchClass(template, notes[2], 1.3);	// step 5 1.3
		} else {
			String shorthand = chord.getShortHand();
			if (Chord.MAJ7.equals(shorthand) || Chord.DOM.equals(shorthand)
					|| Chord.MIN7.equals(shorthand)) {
				fillHarmonicsForPitchClass(template, notes[0], 1.3);
				fillHarmonicsForPitchClass(template, notes[1], 1.0);
				fillHarmonicsForPitchClass(template, notes[2], 1.15);
				fillHarmonicsForPitchClass(template, notes[3], 1.0);
			} else if (Chord.AUG.equals(shorthand) || Chord.DIM.equals(shorthand)
					|| Chord.SUS2.equals(shorthand) || Chord.SUS4.equals(shorthand)) {
				fillHarmonicsForPitchClass(template, notes[0], 1.0);
				fillHarmonicsForPitchClass(template, notes[1], 1.0);
				fillHarmonicsForPitchClass(template, notes[2], 1.0);
			} else {
				throw new IllegalArgumentException("Only plain major/minor chords are supported");
			}
		}
		return template;
	}

	private void fillHarmonicsForPitchClass(double[] template, Note pitchClass, double coefficient) {
		// cannot index java array with negative values, so make offset a positive value
		int fundamentalPitchClass = (pitchClass.offsetFrom(this.startNote) + 12) % 12;
		for (int i = 0; i < HARMONICS_COUNT; i++) {
			template[getPitchClassForIthHarmonic(fundamentalPitchClass, i)] +=
				HARMONIC_CONTRIBUTIONS[i] * coefficient;
		}
	}

	private int getPitchClassForIthHarmonic(int fundamentalPitchClass, int i) {
		if (i < 0) {
			throw new IllegalArgumentException("Harmonic number must be >= 0, but was: " + i);
		}
		return (int)(fundamentalPitchClass + 12 * Math.log(i + 1.0) / Math.log(2.0)) % 12;
	}

}
