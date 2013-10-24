package chordest.transform;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Note;
import chordest.transform.window.HammingWindowFunction;
import chordest.util.QUtil;


public class CQConstants implements Serializable {

	private static final long serialVersionUID = 6005155853894975896L;
	private static final Logger LOG = LoggerFactory.getLogger(CQConstants.class);
	
	public static final double F0_DEFAULT = 440; // Hertz
	
//	/**
//	 * Offset in semitones from base frequency F0 = 440 Hertz which is A5.
//	 * -9 corresponds to C4, -10 corresponds to B3, -21 corresponds to C3 etc.
//	 */
//	public final static int MIN_FREQUENCY_OFFSET_SEMITONES = -33; // 0 = A5, -33 = C2
//	public final static double MIN_FREQUENCY = F0 * 
//			Math.pow(2, MIN_FREQUENCY_OFFSET_SEMITONES / 12.0);
//	public final static Note START_NOTE = Note.getNoteByNumber(
//			MIN_FREQUENCY_OFFSET_SEMITONES);
//	public final static int START_OCTAVE = MIN_FREQUENCY_OFFSET_SEMITONES > 0 ? 
//			(MIN_FREQUENCY_OFFSET_SEMITONES+9)/12 + 5 : 
//			(MIN_FREQUENCY_OFFSET_SEMITONES-2)/12 + 5;
	
//	private static final CQConstants defaultInstance =
//		CQConstants.newInstance(44100, new ScaleInfo(8, 12), -45);
	private static int lastInstanceHash = 0;
//		calculateHash(44100, new ScaleInfo(8, 12), -45);
	
	private static CQConstants lastInstance = null;
	
	private final int componentsTotal;
	private final double minimalFrequency;
	private final Note startNote;
	private final double Q;

	public final int samplingRate;
	public double[][] sinuses;
	public double[][] cosinuses;
	public final double [] componentFrequencies;
	public final int [] componentWindowLengths;
	public final double[][] windowFunctions;

	private CQConstants(int rate, ScaleInfo scaleInfo, double f0,
			int startNoteOffsetInSemitonesFromF0, boolean reduce) {
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		this.samplingRate = rate;
		this.componentsTotal = reduce ? scaleInfo.octaves * 12 : scaleInfo.getTotalComponentsCount();
		this.minimalFrequency = f0 * Math.pow(2, startNoteOffsetInSemitonesFromF0 / 12.0);
//		this.startOctave = startNoteOffsetInSemitonesFromF0 > 0 ? 
//				(startNoteOffsetInSemitonesFromF0 + 9) / 12 + 5 : 
//				(startNoteOffsetInSemitonesFromF0 - 2) / 12 + 5;
		this.startNote = Note.byNumber(startNoteOffsetInSemitonesFromF0);
		this.Q = QUtil.calculateQ(scaleInfo.notesInOctave);
		
		LOG.debug("CQConstants initialization");
		componentFrequencies = initializeComponentFrequencies(reduce ? 12 : scaleInfo.notesInOctave);
		componentWindowLengths = initializeComponentWindowLengths();
		windowFunctions = initializeWindowFunctions();
		initializeSinusesAndCosinuses();
	}

	/**
	 * Calls CQConstants.getInstance(44100, new ScaleInfo(8, 12), -57);
	 * @return CQConstants instants for "default" parameters: 44100 Hz 
	 * sampling rate, ranging at 8 octaves (each of 12 notes) starting from C1
	 */
	public static CQConstants getDefaultInstance() {
		return CQConstants.getInstance(44100, new ScaleInfo(8, 12), F0_DEFAULT, -57);
	}

	public static CQConstants getInstance(int rate, ScaleInfo scaleInfo, double f0,
			int startNoteOffsetInSemitonesFromF0) {
		return getInstance(rate, scaleInfo, f0, startNoteOffsetInSemitonesFromF0, false);
	}
	
	public static CQConstants getInstance(int rate, ScaleInfo scaleInfo, double f0,
			int startNoteOffsetInSemitonesFromF0, boolean reduce) {
		int hash = calculateHash(rate, scaleInfo, f0, startNoteOffsetInSemitonesFromF0, reduce);
		if (lastInstanceHash != hash) {
			lastInstance = newInstance(rate, scaleInfo, f0, startNoteOffsetInSemitonesFromF0, reduce);
			lastInstanceHash = hash;
		}
		return lastInstance;
	}

	private static int calculateHash(int rate, ScaleInfo scaleInfo, double f0,
			int startNoteOffsetInSemitonesFromF0, boolean reduce) {
		return 3 * rate + 5 * scaleInfo.octaves + 
				7 * scaleInfo.notesInOctave + 
				11 * (int)(f0 * 1000) + 
				13 * startNoteOffsetInSemitonesFromF0 +
				(reduce ? 1 : 0);
	}

	private static CQConstants newInstance(int rate, ScaleInfo scaleInfo, double f0,
			int startNoteOffsetInSemitonesFromF0, boolean reduce) {
		return new CQConstants(rate, scaleInfo, f0, startNoteOffsetInSemitonesFromF0, reduce);
	}

	private double[] initializeComponentFrequencies(int notesInOctave) {
		double[] result = new double[this.componentsTotal];
		final double p = 1.0 / notesInOctave;
		for (int i = 0; i < this.componentsTotal; i++) {
			result[i] = Math.pow(2.0, i * p) * this.minimalFrequency;
		}
		return result;
	}

	private int[] initializeComponentWindowLengths() {
		int[] result = new int[this.componentsTotal];
		double qs = this.Q * samplingRate;
		for (int i = 0; i < this.componentsTotal; i++) {
			result[i] = (int)(qs / componentFrequencies[i]);
		}
		return result;
	}

	private double[][] initializeWindowFunctions() {
		double[][] result = new double[this.componentsTotal][];
		for (int i = 0; i < this.componentsTotal; i++) {
			result[i] = new HammingWindowFunction(componentWindowLengths[i]).values;
		}
		return result;
	}

	private void initializeSinusesAndCosinuses() {
		sinuses = new double[this.componentsTotal][];
		cosinuses = new double[this.componentsTotal][];
		for (int i = 0; i < this.componentsTotal; i++) {
			int Nkcq = componentWindowLengths[i];
			double commonArgPart = - 2.0 * Math.PI * Q / Nkcq;
			sinuses[i] = new double[Nkcq];
			cosinuses[i] = new double[Nkcq];
			for (int j = 0; j < Nkcq; j++) {
				double arg = commonArgPart * j;
				sinuses[i][j] = Math.sin(arg);
				cosinuses[i][j] = Math.cos(arg);
			}
		}
	}

	public double getQ() {
		return Q;
	}

	public Note getStartNote() {
		return startNote;
	}

	public int getSamplingRate() {
		return samplingRate;
	}

	public int getLongestWindow() {
		return componentWindowLengths[0];
	}

}
