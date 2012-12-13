package chordest.transform;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Note;
import chordest.transform.window.HammingWindowFunction;
import chordest.transform.window.IWindowFunction;
import chordest.util.ComplexNumber;
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
	
	private final int samplingRate;
	private final int componentsTotal;
	private final ScaleInfo scaleInfo;
	private final double minimalFrequency;
	private final Note startNote;
	private final double Q;

	private ComplexNumber [][] kernels;
	private double[][] sinuses;
	private double[][] cosinuses;
	private double [] componentFrequencies;
	private int [] componentWindowLengths;
	private IWindowFunction [] windowFunctions;

	private CQConstants(int rate, ScaleInfo scaleInfo, double f0, int startNoteOffsetInSemitonesFromF0) {
		if (scaleInfo == null) {
			throw new NullPointerException("scaleInfo is null");
		}
		this.samplingRate = rate;
		this.componentsTotal = scaleInfo.getNotesInOctaveCount() * scaleInfo.getOctavesCount();
		this.scaleInfo = scaleInfo;
		this.minimalFrequency = f0 * Math.pow(2, startNoteOffsetInSemitonesFromF0 / 12.0);
//		this.startOctave = startNoteOffsetInSemitonesFromF0 > 0 ? 
//				(startNoteOffsetInSemitonesFromF0 + 9) / 12 + 5 : 
//				(startNoteOffsetInSemitonesFromF0 - 2) / 12 + 5;
		this.startNote = Note.byNumber(startNoteOffsetInSemitonesFromF0);
		this.Q = QUtil.calculateQ(scaleInfo.getNotesInOctaveCount());
		initialize();
	}

	/**
	 * Calls CQConstants.getInstance(44100, new ScaleInfo(8, 12), -57);
	 * @return CQConstants instants for "default" parameters: 44100 Hz 
	 * sampling rate, ranging at 8 octaves (each of 12 notes) starting from C1
	 */
	public static CQConstants getDefaultInstance() {
		return CQConstants.getInstance(44100, new ScaleInfo(8, 12), F0_DEFAULT, -57);
	}

	public static CQConstants getInstance(int rate, ScaleInfo scaleInfo, double f0, int startNoteOffsetInSemitonesFromF0) {
		int hash = calculateHash(rate, scaleInfo, f0, startNoteOffsetInSemitonesFromF0);
		if (lastInstanceHash != hash) {
			lastInstance = newInstance(rate, scaleInfo, f0, startNoteOffsetInSemitonesFromF0);
			lastInstanceHash = hash;
		}
		return lastInstance;
	}

	private static int calculateHash(int rate, ScaleInfo scaleInfo, double f0, int startNoteOffsetInSemitonesFromF0) {
		return 3 * rate + 5 * scaleInfo.getOctavesCount() + 
				7 * scaleInfo.getNotesInOctaveCount() + 
				11 * (int)(f0 * 1000) + 
				11 * startNoteOffsetInSemitonesFromF0;
	}

	private static CQConstants newInstance(int rate, ScaleInfo scaleInfo, double f0, int startNoteOffsetInSemitonesFromF0) {
		return new CQConstants(rate, scaleInfo, f0, startNoteOffsetInSemitonesFromF0);
	}

	private void initialize() {
		LOG.debug("CQConstants initialization");
		initializeComponentFrequencies();
		initializeComponentWindowLengths();
		initializeWindowFunctions();
		initializeSinusesAndCosinuses();
	}

	private void initializeComponentFrequencies() {
		componentFrequencies = new double[this.componentsTotal];
		for (int i = 0; i < this.componentsTotal; i++) {
			componentFrequencies[i] = Math.pow(2.0, 
					i * 1.0 / this.scaleInfo.getNotesInOctaveCount()) * this.minimalFrequency;
		}
	}

	private void initializeComponentWindowLengths() {
		componentWindowLengths = new int[this.componentsTotal];
		for (int i = 0; i < this.componentsTotal; i++) {
			componentWindowLengths[i] = getWindowLengthForFrequencyAndSamplingRate(
					getFrequencyForComponent(i), this.samplingRate);
		}
	}

	private void initializeWindowFunctions() {
		windowFunctions = new IWindowFunction[this.componentsTotal];
		for (int i = 0; i < this.componentsTotal; i++) {
			windowFunctions[i] = new HammingWindowFunction(getWindowLengthForComponent(i));
		}
	}

	private void initializeSinusesAndCosinuses() {
		sinuses = new double[this.componentsTotal][];
		cosinuses = new double[this.componentsTotal][];
		for (int i = 0; i < this.componentsTotal; i++) {
			int Nkcq = getWindowLengthForComponent(i);
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

	public int getWindowLengthForFrequencyAndSamplingRate(double frequency, double rate) {
		return (int)(this.Q * rate / frequency);
	}

	public double getFrequencyForComponent(int componentNumber) {
		return componentFrequencies[componentNumber];
	}

	public int getWindowLengthForComponent(int componentNumber) {
		return componentWindowLengths[componentNumber];
	}

	public IWindowFunction getWindowFunctionForComponent(int kcq) {
		return windowFunctions[kcq];
	}

	public ComplexNumber getKernel(int kcq, int k) {
		return this.kernels[kcq][k];
	}

	public double getSinus(int componentNumber, int n) {
		return this.sinuses[componentNumber][n];
	}

	public double getCosinus(int componentNumber, int n) {
		return this.cosinuses[componentNumber][n];
	}

	public double[] getComponentFrequencies() {
		return this.componentFrequencies;
	}

	public double getQ() {
		return Q;
	}

	public Note getStartNote() {
		return startNote;
	}

}
