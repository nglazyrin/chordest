package chordest.util;

import chordest.model.Note;
import chordest.transform.ScaleInfo;

public class NoteLabelProvider {

	public static String[] getNoteLabels(int startNoteOffsetInSemitonesFromF0, ScaleInfo scaleInfo) {
		int startOctave = startNoteOffsetInSemitonesFromF0 > 0 ? 
				(startNoteOffsetInSemitonesFromF0 + 9) / 12 + 5 : 
				(startNoteOffsetInSemitonesFromF0 - 2) / 12 + 5;
		Note startNote = Note.byNumber(startNoteOffsetInSemitonesFromF0);
		Note currentNote = startNote;
		int currentOctave = startOctave;
		int subNotes = scaleInfo.notesInOctave / 12;
		String[] result = new String[scaleInfo.getTotalComponentsCount()];
		for (int i = 0; i < scaleInfo.getTotalComponentsCount(); i++) {
			if (i % subNotes == 0) {
				result[i] = currentNote.toString() + currentOctave;
				currentNote = currentNote.next();
				if (Note.C.equals(currentNote)) {
					currentOctave++;
				}
			} else {
				result[i] = "";
			}
		}
		return result;
	}

}
