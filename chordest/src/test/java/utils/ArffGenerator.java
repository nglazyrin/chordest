package utils;

import java.io.File;
import java.util.List;

import chordest.arff.ArffFileWriter;
import chordest.lab.LabFileReader;
import chordest.spectrum.SpectrumData;
import chordest.spectrum.SpectrumFileReader;
import chordest.util.InstanceUtils;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

import weka.core.Instances;

public class ArffGenerator {

	private static final String SEP = PathConstants.SEP;
	private static final String ARTIST = "Beatles";
//	private static final String ALBUM = "01_-_Please_Please_Me";
	private static final String ALBUM = "02_-_With_the_Beatles";
	private static final String PREFIX = ARTIST + SEP + ALBUM + SEP;
	private static final String LAB_DIR = PathConstants.LAB_DIR + PREFIX;
	private static final String SPECTRUM_DIR = "spectrum_tuning" + SEP;

	public static void generate(String spectrumFilePath, String labFilePath, String arffFilePath) {
		LabFileReader labReader = new LabFileReader(new File(labFilePath));
		SpectrumData spectrumData = SpectrumFileReader.read(spectrumFilePath);
		Instances instances = InstanceUtils.buildClassified(
				spectrumData, labReader.getTimestamps(), labReader.getChords());
		instances = InstanceUtils.scaleEach(instances);
		ArffFileWriter.write(arffFilePath, instances);
	}

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(
				new File(LAB_DIR), PREFIX);
		for (String track : tracklist) {
			String labFilePath = PathConstants.LAB_DIR + track;
			String spectrumFilePath = SPECTRUM_DIR + 
					track.replace(PathConstants.EXT_LAB, PathConstants.EXT_BIN);
			String arffFilePath = PathConstants.ARFF_DIR + 
					track.replace(PathConstants.EXT_LAB, PathConstants.EXT_ARFF);
			ArffGenerator.generate(spectrumFilePath, labFilePath, arffFilePath);
		}
	}

}
