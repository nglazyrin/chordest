package chordest.chord.recognition;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Note;
import chordest.transform.ScaleInfo;
import chordest.util.Visualizer;

public class ExtraTemplatesRecognition implements IChordRecognition {

	protected static final Logger LOG = LoggerFactory.getLogger(ExtraTemplatesRecognition.class);

	private final ExtraTemplateProducer producer;

	public ExtraTemplatesRecognition(Note startNote, int templateSize) {
		producer = new ExtraTemplateProducer(startNote, templateSize);
	}

	@Override
	public Chord[] recognize(double[][] cqtSpectrum, ScaleInfo scaleInfo) {
		LOG.debug("Performing recognition...");
		Chord[] result = new Chord[cqtSpectrum.length];
		for (int i = 0; i < cqtSpectrum.length; i++) {
			result[i] = recognize(cqtSpectrum[i]);
		}
		return result;
	}

	private Chord recognize(final double[] cqtSpectrum) {
		final double[] vector = AbstractChordRecognition.metric.normalize(cqtSpectrum);
		Chord nearest = null;
		double minDistance = Double.MAX_VALUE;
		for (Chord chord : TemplatesRecognition.knownChords) {
			if (chord.isEmpty()) {
				continue;
			}
			for (double[] template : producer.getTemplatesFor(chord)) {
				double d = AbstractChordRecognition.metric.distance(template, vector);
				if (d < minDistance) {
					minDistance = d;
					nearest = chord;
				}
			}
		}
		return nearest;
	}

	public static class ExtraTemplateProducer {

		private static final int HARMONICS_COUNT = 3;
		private static final double[] HARMONIC_CONTRIBUTIONS = new double[HARMONICS_COUNT];
		private static final double CONTRIBUTION_REDUCTION = 0.6;

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

		private final int templateSize;

		private Map<Chord, double[][]> templates = new HashMap<Chord, double[][]>();

		public ExtraTemplateProducer(Note startNote, int templateSize) {
			this.startNote = startNote;
			this.templateSize = templateSize;
			if (templateSize % 12 != 0) {
				throw new IllegalArgumentException("template size must be multiple of 12");
			}
			int octaves = templateSize / 12;
			for (Chord chord : TemplatesRecognition.knownChords) {
				if (chord.isEmpty()) {
					continue;
				}
				Note[] notes = chord.getNotes();
				double[][] result = new double[(int) Math.pow(octaves, notes.length)][templateSize];
				int index = 0;
				for (int o0 = 0; o0 < octaves; o0++) {
					for (int o1 = 0; o1 < octaves; o1++) {
						for (int o2 = 0; o2 < octaves; o2++) {
							if (notes.length == 4) {
								for (int o3 = 0; o3 < octaves; o3++) {
									result[index++] = getTemplateFor(chord, new int[] { o0, o1, o2, o3 });
								}
							} else {
								result[index++] = getTemplateFor(chord, new int[] { o0, o1, o2 });
							}
						}
					}
				}
				templates.put(chord, result);
				Visualizer.visualizeXByFrequencyDistribution(result[1], new ScaleInfo(4, 12), -33);
			}
		}

		public double[][] getTemplatesFor(Chord chord) {
			return templates.get(chord);
		}

		private double[] getTemplateFor(Chord chord, int[] octaves) {
			double[] template = new double[templateSize];
			Note[] notes = chord.getNotes();
			if (chord.isMajor() || chord.isMinor()) {
				fillHarmonicsForPitchClass(template, notes[0], HARMONICS_COUNT, octaves[0], 1.0);
				fillHarmonicsForPitchClass(template, notes[1],HARMONICS_COUNT, octaves[1], 1.0);
				fillHarmonicsForPitchClass(template, notes[2],HARMONICS_COUNT, octaves[2], 1.0);
			} else if (chord.isEmpty()) {
				// fill nothing
			} else {
				String shorthand = chord.getShortHand();
				if (Chord.MAJ7.equals(shorthand) || Chord.DOM.equals(shorthand)
						|| Chord.MIN7.equals(shorthand)) {
					fillHarmonicsForPitchClass(template, notes[0], 2, octaves[0], 1.0);
					fillHarmonicsForPitchClass(template, notes[1], 2, octaves[1], 1.0);
					fillHarmonicsForPitchClass(template, notes[2], 2, octaves[2], 1.0);
					fillHarmonicsForPitchClass(template, notes[3], 2, octaves[3], 1.0);
				} else if (Chord.AUG.equals(shorthand) || Chord.DIM.equals(shorthand)
						|| Chord.SUS2.equals(shorthand) || Chord.SUS4.equals(shorthand)) {
					fillHarmonicsForPitchClass(template, notes[0], HARMONICS_COUNT, octaves[0], 1.0);
					fillHarmonicsForPitchClass(template, notes[1], HARMONICS_COUNT, octaves[1], 1.0);
					fillHarmonicsForPitchClass(template, notes[2], HARMONICS_COUNT, octaves[2], 1.0);
				} else {
					throw new IllegalArgumentException("Only plain major/minor chords are supported");
				}
			}
			return AbstractChordRecognition.metric.normalize(template);
		}

		private void fillHarmonicsForPitchClass(double[] template, Note pitchClass,
				int count, int octave, double coefficient) {
			// cannot index java array with negative values, so make offset a positive value
			int fundamentalPitchClass = (pitchClass.offsetFrom(this.startNote) + 12) % 12;
			for (int i = 0; i < count; i++) {
				int index = getPitchClassForIthHarmonic(fundamentalPitchClass, i, octave);
				if (index >= 0 && index < template.length) {
					template[index] += HARMONIC_CONTRIBUTIONS[i] * coefficient;
				}
			}
		}

		private int getPitchClassForIthHarmonic(int fundamentalPitchClass, int i, int octave) {
			if (i < 0) {
				throw new IllegalArgumentException("Harmonic number must be >= 0, but was: " + i);
			}
			return (int)(fundamentalPitchClass + 12 * Math.log(i + 1.0) / Math.log(2.0)) + octave * 12;
		}

	}

}
