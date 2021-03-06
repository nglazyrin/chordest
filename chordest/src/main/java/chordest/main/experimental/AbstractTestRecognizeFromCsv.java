package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import chordest.chord.comparison.ChordListsComparison;
import chordest.io.csv.CsvFileWriter;
import chordest.io.lab.LabFileReader;
import chordest.io.lab.LabFileWriter;
import chordest.io.spectrum.SpectrumFileReader;
import chordest.main.Roundtrip;
import chordest.model.Chord;
import chordest.spectrum.SpectrumData;
import chordest.util.DataUtil;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public abstract class AbstractTestRecognizeFromCsv extends Roundtrip {

	private static String OUTPUT_DIRECTORY = "work" + PathConstants.SEP + "lab" + PathConstants.SEP;

	public abstract String getCsvDirectory();

	public abstract Chord[] recognize(File csvFile);

	public void recognizeFromCsv() {
		writeCsvHeaders();
		for (int index = 0; index < TrainTestDataCircularGenerator.PARTS; index++) {
			List<String> tracklist = TracklistCreator.readTrackList(
					TrainTestDataCircularGenerator.getTestFileListName(index));
			for (String item : tracklist) {
				String track = StringUtils.substringAfterLast(item, PathConstants.SEP);
				String binFile = item;
				String csvFile = getCsvDirectory() + index + "\\" + track + PathConstants.EXT_CSV;
				String labFile = PathConstants.LAB_DIR + StringUtils.replace(track, PathConstants.EXT_WAV + PathConstants.EXT_BIN, PathConstants.EXT_LAB);
				doChordRecognition(binFile, csvFile, labFile);
			}
		}
		writeStatistics();
	}

	private void doChordRecognition(String binFile, String csvFile, String expectedLab) {
		SpectrumData sd = SpectrumFileReader.read(binFile);
		Chord[] chords = recognize(new File(csvFile));
		double[] beatTimes = DataUtil.toAllBeatTimes(sd.beatTimes, sd.framesPerBeat);
		
		LabFileWriter lw = new LabFileWriter(chords, beatTimes);
		File temp = null;
		try {
			temp = new File(OUTPUT_DIRECTORY, new File(expectedLab).getName());
			if (temp.exists()) { FileUtils.forceDelete(temp); }
			lw.writeTo(temp);
		} catch (IOException e) {
			LOG.error("Error when saving temporary lab file", e);
			System.exit(-1);
		}
		
		LabFileReader labReaderExpected = new LabFileReader(new File(expectedLab));
		LabFileReader labReaderActual = new LabFileReader(temp);
		
		String track = StringUtils.substringAfterLast(expectedLab, PathConstants.SEP);
		String csvFileName = StringUtils.replace(track, PathConstants.EXT_LAB, PathConstants.EXT_CSV);
		write(new CsvFileWriter(labReaderActual.getChords(), labReaderActual.getTimestamps()), Roundtrip.CSV_TRIADS_DIR + csvFileName);
		write(new CsvFileWriter(labReaderExpected.getChords(), labReaderExpected.getTimestamps()), Roundtrip.CSV_EXPECTED_DIR + csvFileName);
		
		ChordListsComparison[] cmp = new ChordListsComparison[3];
		for (int i = 0; i < acc.length; i++) {
			cmp[i] = new ChordListsComparison(labReaderExpected.getChords(),
					labReaderExpected.getTimestamps(), labReaderActual.getChords(),
					labReaderActual.getTimestamps(), acc[i].getMetric());
			acc[i].append(cmp[i]);
		}
		
		SIM_LOG.info(String.format("%s;%s;%f;%f;%f;%f;%f;%f", 
				track.replace(',', '_').replace('\\', '/'), null,
				cmp[0].getOverlapMeasure(),
				cmp[1].getOverlapMeasure(),
				cmp[2].getOverlapMeasure(),
				cmp[0].getSegmentation(),
				cmp[2].getEffectiveSeconds(),
				cmp[0].getTotalSeconds()));
		
		LOG.info(String.format("%s: Mirex %.4f; Triads %.4f; Tetrads %.4f; Segm %.4f", 
				track, cmp[0].getOverlapMeasure(), cmp[1].getOverlapMeasure(),
				cmp[2].getOverlapMeasure(), cmp[0].getSegmentation()));
	}

}
