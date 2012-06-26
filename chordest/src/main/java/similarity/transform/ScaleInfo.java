package similarity.transform;

import java.io.Serializable;


public class ScaleInfo implements Serializable {

	private static final long serialVersionUID = -8240598304243514812L;

	private int notesInOctaveCount;
	private int octavesCount;

	public static ScaleInfo getDefaultInstance() {
		return new ScaleInfo(5, 12);
	}

	public ScaleInfo(int octaves, int notesInOctave) {
		this.notesInOctaveCount = notesInOctave;
		this.octavesCount = octaves;
	}

	public int getNotesInOctaveCount() {
		return this.notesInOctaveCount;
	}

	public int getOctavesCount() {
		return this.octavesCount;
	}

	public int getTotalComponentsCount() {
		return notesInOctaveCount * octavesCount;
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
		return otherInfo.notesInOctaveCount == this.notesInOctaveCount &&
				otherInfo.octavesCount == this.octavesCount;
	}

	@Override
	public int hashCode() {
		return octavesCount * 31 + notesInOctaveCount;
	}

	public chordest.transform.ScaleInfo toNewScaleInfo() {
		return new chordest.transform.ScaleInfo(this.octavesCount, this.notesInOctaveCount);
	}

}
