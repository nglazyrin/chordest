package utils;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.Chord;
import chordest.lab.LabFileReader;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;


public class ChordFinder {

	private static final Logger LOG = LoggerFactory.getLogger(ChordFinder.class);

	private static final String SEP = PathConstants.SEP;
	private static final String ARTIST = "Beatles";
	private static final String PREFIX = ARTIST + SEP; //+ ALBUM + SEP;
	private static final String LAB_DIR = PathConstants.LAB_DIR;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(
				new File(LAB_DIR), "");
		String target = "F#,A,D";
		for (String track : tracklist) {
			LabFileReader reader = new LabFileReader(new File(LAB_DIR + track));
			Chord[] chords = reader.getChords();
			for (int i = 0; i < chords.length; i++) {
				Chord chord = chords[i];
				if (target.equals(chord.toString())) {
					LOG.error(reader.getTimestamps()[i] + " of " + track);
					break;
				}
			}
		}
	}

}
