package utils;

import java.io.File;
import java.util.List;
import java.util.Random;

import chordest.arff.ArffFileReader;
import chordest.arff.ArffFileWriter;
import chordest.chord.Key;
import chordest.util.InstanceUtils;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

import weka.core.Instances;

public class ArffCombiner {

	private static final String SEP = PathConstants.SEP;
	private static final String ARTIST = "Beatles";
	private static final String ALBUM = "01_-_Please_Please_Me";
	private static final String PREFIX = ARTIST + SEP; //+ ALBUM + SEP;
	private static final String LAB_DIR = PathConstants.LAB_DIR + PREFIX;
	private static final int LIMIT = 100;
	private static final Key MODE = Key.E_MAJ;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(
				new File(LAB_DIR), PREFIX);
		Instances all = combineForMode(tracklist, MODE);
		all.randomize(new Random(System.currentTimeMillis()));
		all = InstanceUtils.subsetNotMuchThanFixedSize(all, LIMIT);
		ArffFileWriter.write(PathConstants.ARFF_DIR + PREFIX + "emaj" + PathConstants.EXT_ARFF, all);
	}

	private static Instances combine(List<String> tracklist) {
		Instances all = null;
		for (String track : tracklist) {
			String arffFilePath = PathConstants.ARFF_DIR +
					track.replace(PathConstants.EXT_LAB, PathConstants.EXT_ARFF);
			Instances instances = ArffFileReader.read(arffFilePath);
			if (all == null) {
				all = instances;
			} else {
				InstanceUtils.appendInstances(all, instances);
			}
		}
		return all;
	}

	private static Instances combineForMode(List<String> tracklist, Key mode) {
		Instances instances = combine(tracklist);
		instances = InstanceUtils.preserveWithValues(instances, mode.getChords());
		return instances;
	}

}
