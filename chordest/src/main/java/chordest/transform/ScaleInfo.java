package chordest.transform;

import java.io.Serializable;


public class ScaleInfo implements Serializable {

	private static final long serialVersionUID = -8240598304243514812L;

	public final int notesInOctave;
	public final int octaves;

	public static ScaleInfo getDefaultInstance() {
		return new ScaleInfo(5, 12);
	}

	public ScaleInfo(int octaves, int notesInOctave) {
		this.notesInOctave = notesInOctave;
		this.octaves = octaves;
	}

	public int getTotalComponentsCount() {
		return notesInOctave * octaves;
	}

	@Override
	public boolean equals(Object other) {
		if (! (other instanceof ScaleInfo)) {
			return false;
		}
		if (other == this) {
			return true;
		}
		ScaleInfo otherInfo = (ScaleInfo) other;
		return otherInfo.notesInOctave == this.notesInOctave &&
				otherInfo.octaves == this.octaves;
	}

	@Override
	public int hashCode() {
		return octaves * 31 + notesInOctave;
	}

	@Override
	public String toString() {
		return "octaves: " + octaves + ", notes in octave: " + notesInOctave;
	}

}
