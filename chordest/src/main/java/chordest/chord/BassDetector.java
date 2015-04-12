package chordest.chord;

import java.util.Arrays;

import chordest.chord.templates.ITemplateProducer;
import chordest.model.Chord;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.DataUtil;

public class BassDetector {

	private final double[][] spectrum;
	private final ScaleInfo scaleInfo;
	private final int offsetFromF0;

	public BassDetector(double[][] spectrum, ScaleInfo scaleInfo, int offsetFromF0) {
		this.spectrum = spectrum;
		this.scaleInfo = scaleInfo;
		this.offsetFromF0 = offsetFromF0;
	}

	public Chord[] correctTactus(Chord[] chords, ITemplateProducer producer) {
		return TactusCorrector.correct(chords, spectrum, scaleInfo, producer);
	}

	public Chord[] detectBass(Chord[] chords) {
		Chord[] result = Arrays.copyOf(chords, chords.length);
		int i = 0;
		while (i < spectrum.length) {
			Chord chord = chords[i];
			if (chord == null || chord.isEmpty()) {
				result[i] = chord;
				i++;
				continue;
			}
			int j = i;
			while (j < spectrum.length && chord.equals(chords[j])) {
				j++;
			}
			double[] avgSpectrum = DataUtil.sumVectors(spectrum, i, j);
			avgSpectrum = DataUtil.multiply(avgSpectrum, 1.0 / (j + 1 - i));
			int subnotes = scaleInfo.notesInOctave / 12;
			int[] noteOffsets = getNoteOffsets(chord, offsetFromF0);
			int notes = chord.getNotes().length;
			double[] chordSpectrum = new double[notes * scaleInfo.octaves];
			double[] avgByNote = new double[notes];
			double avg = 0;
			for (int octave = 0; octave < scaleInfo.octaves; octave++) {
				for (int n = 0; n < notes; n++) {
					double value = avgSpectrum[octave * scaleInfo.notesInOctave + noteOffsets[n] * subnotes];
					chordSpectrum[octave * notes + n] = value;
					avgByNote[n] += value;
					avg += value;
				}
			}
			avg /= chordSpectrum.length;
			if (notes > 3) {
				double limit = scaleInfo.octaves * avg * 0.8;
				if (avgByNote[0] < limit && avgByNote[notes - 1] > limit) {
					if (chord.isOfType(Chord.MAJ7)) { 
						chord = Chord.minor(chord.getNotes()[1]);
					} else if (chord.isOfType(Chord.MIN7)) {
						chord = Chord.major(chord.getNotes()[1]);
					}
				} else if (avgByNote[notes - 1] < limit && avgByNote[0] > limit) {
					if (chord.isOfType(Chord.MAJ7) || chord.isOfType(Chord.DOM)) { 
						chord = Chord.major(chord.getRoot());
					} else if (chord.isOfType(Chord.MIN7)) {
						chord = Chord.minor(chord.getRoot());
					}
				}
			}
			//for (int n = 0; n < chordSpectrum.length; n++) {
			for (int n = 0; n < notes; n++) {
				if (chordSpectrum[n] >= 1.0 * avg) {
					Note note = Note.A.withOffset(offsetFromF0 + noteOffsets[n % notes]);
					int bassOffset = note.positiveOffsetFrom(chord.getRoot());
					for (int k = i; k < j; k++) {
						result[k] = new Chord(bassOffset, chord.getNotes());
					}
					break;
				}
			}
			i = j;
		}
		return result;
	}

	private static int[] getNoteOffsets(Chord chord, int offsetFromF0) {
		Note[] notes = chord.getNotes();
		int[] noteOffsets = new int[notes.length];
		Note baseNote = Note.A.withOffset(offsetFromF0);
		for (int n = 0; n < notes.length; n++) {
			noteOffsets[n] = notes[n].positiveOffsetFrom(baseNote);
		}
		return noteOffsets;
	}

}
