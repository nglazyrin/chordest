package similarity.arff;

import similarity.chord.Chord;
import similarity.chord.Note;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;

public class WekaData {

	private static FastVector attributes;
	private static Attribute chordAttribute;
	private static FastVector chordLabels;
	
	static {
		Attribute strengthA = new Attribute("strengthA");
		Attribute strengthAd = new Attribute("strengthAd");
		Attribute strengthB = new Attribute("strengthB");
		Attribute strengthC = new Attribute("strengthC");
		Attribute strengthCd = new Attribute("strengthCd");
		Attribute strengthD = new Attribute("strengthD");
		Attribute strengthDd = new Attribute("strengthDd");
		Attribute strengthE = new Attribute("strengthE");
		Attribute strengthF = new Attribute("strengthF");
		Attribute strengthFd = new Attribute("strengthFd");
		Attribute strengthG = new Attribute("strengthG");
		Attribute strengthGd = new Attribute("strengthGd");
		FastVector noteLabels = new FastVector();
		for (int i = 0; i < 12; i++) {
			noteLabels.addElement(Note.byNumber(i).getShortName());
		}
		chordLabels = new FastVector();
		for (int i = 0; i < 12; i++) {
			chordLabels.addElement(Chord.major(Note.byNumber(i)).toString());
			chordLabels.addElement(Chord.minor(Note.byNumber(i)).toString());
		}
		chordLabels.addElement(Chord.empty().toString());
		chordAttribute = new Attribute("chord", chordLabels);
		
		attributes = new FastVector(13);
		attributes.addElement(strengthA);
		attributes.addElement(strengthAd);
		attributes.addElement(strengthB);
		attributes.addElement(strengthC);
		attributes.addElement(strengthCd);
		attributes.addElement(strengthD);
		attributes.addElement(strengthDd);
		attributes.addElement(strengthE);
		attributes.addElement(strengthF);
		attributes.addElement(strengthFd);
		attributes.addElement(strengthG);
		attributes.addElement(strengthGd);
		attributes.addElement(chordAttribute);
	}

	private static Attribute getChordAttribute() {
		return chordAttribute;
	}

	public static FastVector getAttributes() {
		return attributes;
	}

	public static FastVector getChordLabels() {
		return chordLabels;
	}

	public static Instance newClassifiedInstance(double[] strengths, Chord chord) {
		if (strengths.length != 12 || chord == null) {
			//throw new RuntimeException("There should be exactly 12 strengths but was: " + strengths.length);
			return null;
		}
		if (isKnownChord(chord)) {
			double[] values = new double[attributes.size()];
			for (int i = 0; i < 12; i++) {
				values[i] = strengths[i];
			}
			values[12] = WekaData.getChordAttribute().indexOfValue(chord.toString());
			return new Instance(1.0, values);
		} else {
			return null;
		}
	}

	public static Instance newUnclassifiedInstance(double[] strengths) {
		return newClassifiedInstance(strengths, Chord.empty());
	}

	public static String getChordName(double classValue) {
		return getChordAttribute().value((int)classValue);
	}

	public static boolean isKnownChord(Chord chord) {
		return getChordAttribute().indexOfValue(chord.toString()) >= 0;
	}

	public static int indexOf(Chord chord) {
		return WekaData.getChordAttribute().indexOfValue(chord.toString());
	}

}
