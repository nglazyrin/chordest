package experimental;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.model.Chord;
import chordest.model.Key;
import chordest.model.Note;


public class KeyIntersectionTest {

	private static final Logger LOG = LoggerFactory.getLogger(KeyIntersectionTest.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Key[] keys = new Key[12];
		int index = 0;
		for (Note note : Note.values()) {
			keys[index++] = new Key(note, Chord.MAJ);
//			modes[index++] = new Key(note, Chord.MIN);
		}
		int maxCommon = 0;
		Key max1 = null;
		Key max2 = null;
		for (int i = 0; i < 12; i++) {
			Key key1 = keys[i];
			for (int j = i+1; j < 12; j++) {
				Key key2 = keys[j];
				int common = commonNotes(key1, key2);
				if (common > maxCommon) {
					maxCommon = common;
					max1 = key1;
					max2 = key2;
				}
			}
		}
		LOG.info("Max common: " + maxCommon);
		LOG.info("Key 1: " + max1.toString());
		LOG.info("Key 2: " + max2.toString());
	}

	private static int commonNotes(Key key1, Key key2) {
		Set<Note> set = new HashSet<Note>();
		set.addAll(key1.getNotes());
		set.addAll(key2.getNotes());
		return 14 - set.size();
	}

}
