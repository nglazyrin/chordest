package chordest.chord.recognition;

import chordest.model.Chord;
import chordest.transform.ScaleInfo;

public interface IChordRecognition {

	public Chord[] recognize(final double[][] cqtSpectrum, final ScaleInfo scaleInfo);
}
