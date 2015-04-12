package chordest.main.experimental;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import chordest.chord.KeyExtractor;
import chordest.chord.templates.ITemplateProducer;
import chordest.chord.templates.TemplateProducer;
import chordest.configuration.Configuration;
import chordest.io.csv.CsvSpectrumFileReader;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.main.Roundtrip;
import chordest.model.Key;
import chordest.model.Note;
import chordest.spectrum.SpectrumData;
import chordest.util.DataUtil;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class TestCsvChroma extends Roundtrip {

//	private static String CSV_DIRECTORY = "work\\dissertation\\sda\\sda.60-300-300\\encoded";
	private static String CSV_DIRECTORY = "E:\\Dev\\Python\\pylearn-test\\models\\sda.48-100\\test";

	public static void main(String[] args) {
		String labDir;
		if (args.length > 1) {
			c = new Configuration(args[0]);
			labDir = args[1];
		} else {
			c = new Configuration();
			labDir = PathConstants.LAB_DIR;
		}
		writeCsvHeaders();
		for (int index = 0; index < TrainTestDataCircularGenerator.PARTS; index++) {
			List<String> tracklist = TracklistCreator.readTrackList(
					TrainTestDataCircularGenerator.getTestFileListName(index));
			for (String item : tracklist) {
				String temp = StringUtils.substringAfterLast(item, PathConstants.SEP);
				String track = StringUtils.substringBeforeLast(temp, PathConstants.EXT_WAV + PathConstants.EXT_BIN);
				String binFile = item;
				String csvFile = CSV_DIRECTORY + index + "\\" + track + PathConstants.EXT_WAV + PathConstants.EXT_BIN + PathConstants.EXT_CSV;
				SpectrumData sd = SpectrumFileReader.read(binFile);
				double[][] chroma = recognize(new File(csvFile));
				Key key = KeyExtractor.getKey(chroma, Note.byNumber(sd.startNoteOffsetInSemitonesFromF0));
				double[] noChordness = getNoChordness(chroma);
				double[] beatTimes = DataUtil.toAllBeatTimes(sd.beatTimes, sd.framesPerBeat);
				ITemplateProducer producer = new TemplateProducer(Note.byNumber(c.spectrum.offsetFromF0InSemitones), c.template);
				processFile(chroma, noChordness, null, beatTimes, key, labDir, track, acc, producer);
			}
		}
		writeStatistics();
	}

	private static double[] getNoChordness(double[][] chroma) {
		double[] noChordness = new double[chroma.length];
		for (int i = 0; i < chroma.length; i++) {
			double[] t = chroma[i];
			double max = 0;
			for (int j = 0; j < t.length; j++) {
				max = Math.max(Math.abs(t[j]), max);
			}
			if (max < 0.1) {
				noChordness[i] = 0;
			} else {
				noChordness[i] = 1;
			}
		}
		return noChordness;
	}

	private static double[][] recognize(File csvFile) {
		double[][] chroma = new CsvSpectrumFileReader(csvFile).getSpectrum();
		DataUtil.scaleEachTo01(chroma);
		
		double[][] selfSim = DataUtil.getSelfSimilarity(chroma); // TODO
		selfSim = DataUtil.removeDissimilar(selfSim, c.process.selfSimilarityTheta);
		chroma = DataUtil.smoothWithSelfSimilarity(chroma, selfSim);
		return chroma;
	}

}
