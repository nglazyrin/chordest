package chordest.main.experimental;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.chord.comparison.ChordListsComparison;
import chordest.chord.comparison.ComparisonAccumulator;
import chordest.chord.comparison.Mirex2010;
import chordest.chord.comparison.Tetrads;
import chordest.chord.comparison.Triads;
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

public abstract class AbstractTestRecognizeFromCsv {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractTestRecognizeFromCsv.class);
	private static final Logger ERR_LOG = LoggerFactory.getLogger("Errors");
	private static final Logger SIM_LOG = LoggerFactory.getLogger("Similarity");

	private static String OUTPUT_DIRECTORY = "work" + PathConstants.SEP + "lab" + PathConstants.SEP;

	private static ComparisonAccumulator[] acc = new ComparisonAccumulator[] {
		new ComparisonAccumulator(new Mirex2010()),
		new ComparisonAccumulator(new Triads()),
		new ComparisonAccumulator(new Tetrads())
	};

	public abstract String getCsvDirectory();

	public abstract Chord[] recognize(File csvFile);

	public void recognizeFromCsv() {
		ERR_LOG.info("metric;type;total;0;1;2;3;4;5;6;7;8;9;10;11");
		SIM_LOG.info("name;key;overlapM;overlap3;overlap4;segmentation;effective_length;full_length");
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

		for (int i = 0; i < acc.length; i++) {
			LOG.info("");
			acc[i].printStatistics(LOG);
			acc[i].printErrorStatistics(ERR_LOG);
		}
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
		Roundtrip.write(new CsvFileWriter(labReaderActual.getChords(), labReaderActual.getTimestamps()), Roundtrip.CSV_ACTUAL_DIR + csvFileName);
		Roundtrip.write(new CsvFileWriter(labReaderExpected.getChords(), labReaderExpected.getTimestamps()), Roundtrip.CSV_EXPECTED_DIR + csvFileName);
		
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
