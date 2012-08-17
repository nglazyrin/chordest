package utils;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYZDataset;

import chordest.chord.Note;
import chordest.gui.JFreeChartUtils;
import chordest.lab.LabFileReader;
import chordest.properties.Configuration;
import chordest.transform.ScaleInfo;
import chordest.util.DatasetUtil;
import chordest.util.NoteLabelProvider;


public class LabViewer {

	private static final String SEP = File.separator;
	private static final String EXT_LAB = ".lab";
	private static final String ARTIST = "Beatles";
	private static final String ALBUM = "03_-_A_Hard_Day's_Night";
	private static final String TRACK = "04_-_I'm_Happy_Just_To_Dance_With_You";
	private static final String LAB_EXPECTED_FILENAME = ".." + SEP + "mir" + SEP + 
			"resources" + SEP + "lab" + SEP + ARTIST + SEP + ALBUM + SEP + 
			TRACK + EXT_LAB;
	private static final String LAB_ACTUAL_FILENAME = "lab" + SEP + ARTIST +
			SEP + ALBUM + SEP + TRACK + EXT_LAB;

	public static void main(String[] args) {
		LabFileReader eReader = new LabFileReader(new File(LAB_EXPECTED_FILENAME));
		LabFileReader aReader = new LabFileReader(new File(LAB_ACTUAL_FILENAME));
		Configuration c = new Configuration();
		try {
			String[] labels = NoteLabelProvider.getNoteLabels(c.spectrum.offsetFromF0InSemitones,
					new ScaleInfo(c.spectrum.octaves, c.spectrum.notesPerOctave));
			XYZDataset eds = DatasetUtil.toXYZDataset(
					Arrays.copyOfRange(eReader.getTimestamps(), 0, eReader.getChords().length),
					eReader.getChords(), Note.C);
			JFreeChartUtils.visualizeStringY("Expected", "Time", "Notes", eds, 1200, 300, labels);
			XYZDataset ads = DatasetUtil.toXYZDataset(
					Arrays.copyOfRange(aReader.getTimestamps(), 0, aReader.getChords().length),
					aReader.getChords(), Note.C);
			JFreeChartUtils.visualizeStringY("Actual", "Time", "Notes", ads, 1200, 300, labels);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

}
