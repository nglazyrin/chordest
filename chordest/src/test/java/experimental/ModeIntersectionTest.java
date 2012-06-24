package experimental;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import similarity.chord.Chord;
import similarity.chord.Mode;
import similarity.chord.Note;

public class ModeIntersectionTest {

	private static final Logger LOG = LoggerFactory.getLogger(ModeIntersectionTest.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Mode[] modes = new Mode[12];
		int index = 0;
		for (Note note : Note.values()) {
			modes[index++] = new Mode(note, Chord.MAJ);
//			modes[index++] = new Mode(note, Chord.MIN);
		}
		int maxCommon = 0;
		Mode max1 = null;
		Mode max2 = null;
		for (int i = 0; i < 12; i++) {
			Mode mode1 = modes[i];
			for (int j = i+1; j < 12; j++) {
				Mode mode2 = modes[j];
				int common = commonNotes(mode1, mode2);
				if (common > maxCommon) {
					maxCommon = common;
					max1 = mode1;
					max2 = mode2;
				}
			}
		}
		LOG.info("Max common: " + maxCommon);
		LOG.info("Mode 1: " + max1.toString());
		LOG.info("Mode 2: " + max2.toString());
	}

	private static int commonNotes(Mode mode1, Mode mode2) {
		Set<Note> set = new HashSet<Note>();
		set.addAll(mode1.getNotes());
		set.addAll(mode2.getNotes());
		return 14 - set.size();
	}

}
