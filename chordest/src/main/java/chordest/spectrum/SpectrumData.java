package chordest.spectrum;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;

import chordest.transform.ScaleInfo;



public class SpectrumData implements Serializable {

	private static final long serialVersionUID = 6953384572465365607L;

	/**
	 * Array of beat times used when calculating this spectrum
	 */
	public double[] beatTimes;
	
	/**
	 * The tuning frequency. Usually it is equal to 440 Hz, but not always
	 */
	public double f0;
	
	/**
	 * Sampling rate of the sound file, often it is equal to 44100 Hz
	 */
	public int samplingRate;
	
	/**
	 * The number of octaves and the number of notes in each octave, which
	 * were used when calculating this spectrum
	 */
	public ScaleInfo scaleInfo;
	
	/**
	 * The actual spectrum as an array of columns, one column per beat
	 */
	public double[][] spectrum;
	
	/**
	 * F0 usually corresponds to A4. This parameter allows you to set another
	 * note as a starting point on frequency axis for the transforms. The 0th
	 * component of each column in spectrum array corresponds to this note.
	 */
	public int startNoteOffsetInSemitonesFromF0;
	
	/**
	 * Total sound length in seconds
	 */
	public double totalSeconds;
	
	/**
	 * The relative path to the sound file for which this spectrum was calculated
	 */
	public String wavFilePath;

	public boolean equalsIgnoreSpectrumAndF0(SpectrumData other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		EqualsBuilder builder = new EqualsBuilder();
		builder.append(samplingRate, other.samplingRate);
		builder.append(totalSeconds, other.totalSeconds);
//		builder.append(f0, other.f0);
		builder.append(scaleInfo, other.scaleInfo);
		builder.append(startNoteOffsetInSemitonesFromF0, other.startNoteOffsetInSemitonesFromF0);
//		builder.append(wavFilePath, other.wavFilePath);
		builder.append(beatTimes.length, other.beatTimes.length); // weak check to avoid double == comparison
		return builder.isEquals();
	}

}
