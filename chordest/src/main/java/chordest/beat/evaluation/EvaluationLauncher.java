package chordest.beat.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.beat.BeatRootBeatTimesProvider;
import chordest.beat.MyBeatTimesProvider;
import chordest.io.beat.BeatTimesFileReader;
import chordest.io.beat.SingleRowBeatTimesFileReader;
import chordest.util.Visualizer;

public class EvaluationLauncher {

	private static final Logger LOG = LoggerFactory.getLogger(MyBeatTimesProvider.class);
	
	public static void main(String[] args) throws IOException {
		
		// http://www.music-ir.org/mirex/wiki/2014:Audio_Beat_Tracking
		// in the bottom of page you can find test set of short musical excerpts
		
		// look through all files in directory of test set
		String testDirectory = "D:/USU/MIR/MIREX '11/beattrack_train_2006/train/";
		
		// fmeasure output file
		String fmeasureFileName = testDirectory + "fmeasure.txt";
		File fmeasureFile = new File(fmeasureFileName);
		Writer fmeasureWriter = new FileWriter(fmeasureFile);
		
		// cemgil output file
		String cemgilFileName = testDirectory + "cemgil.txt";
		File cemgilFile = new File(cemgilFileName);
		Writer cemgilWriter = new FileWriter(cemgilFile);
		
		LOG.info("Start processing...");
		
		double average = 0;
		double averageCemgil = 0;
		// count of files in test directory
		int countOfTestSongs = 20;
		for (int i=1; i<=countOfTestSongs; ++i)
		{
			String expectedFileName = testDirectory + "train" + i + ".txt";
			File expFile = new File(expectedFileName);
			
			BeatTimesFileReader expReader = new BeatTimesFileReader(expFile);
			double[][] expectedBeats = expReader.getBeatTimes();
			
			// alternative way to represent all 40 excerpts in single sequence
			// SingleRowBeatTimesFileReader expReader = new SingleRowBeatTimesFileReader(expFile);
			// double[] expectedBeats = expReader.getBeatTimes();
			
			String actualFileName = testDirectory + "train" + i + ".wav";
			MyBeatTimesProvider mbtp = new MyBeatTimesProvider(actualFileName, "", "", false);
			double[] actualBeats = mbtp.getBeatTimes();
			
			// draw all beats in single row
			/*
			double WINDOW_STEP = 0.001;
			double[] windowBeginnings = BeatRootBeatTimesProvider.generateTimeScale(actualFileName, WINDOW_STEP);
			double[] singleRow = new double[windowBeginnings.length];
			for (int j = 0; j < singleRow.length; j++) {
				singleRow[j] = 0;
			}
			// uncomment this when alternative way used
 			// for (int j = 0; j < expectedBeats[0].length; j++) {
 			for (int j = 0; j < expectedBeats.length; j++) {
				// uncomment this when alternative way used
 				// int index = (int) Math.floor(expectedBeats[0][j] / WINDOW_STEP); 
 				int index = (int) Math.floor(expectedBeats[j] / WINDOW_STEP);
				singleRow[index] = 1000;
			}
 			for (int j = 0; j < actualBeats.length; j++) {
 				int index = (int) Math.floor(actualBeats[j] / WINDOW_STEP); 
				singleRow[index] = 3000;
 			}
			Visualizer.visualizeXByTimeDistribution(singleRow, windowBeginnings);
			*/
			
			// use this for average result of comparing
			// double percent = calcSimilarity(new FMeasureEvaluation(), expectedBeats, actualBeats);
			
			// use this similarity method if alternative way is used
			// double percent = calcSimilaritySingleRow(new FMeasureEvaluation(), expectedBeats, actualBeats);
			
			// this way is recommended by mirex community
			double percent = calcSimilarityMax(new FMeasureEvaluation(), expectedBeats, actualBeats);
			
			average += percent;
			fmeasureWriter.write(String.format("%.2f\r\n", percent));
			
			percent = calcSimilarityMax(new CemgilEvaluation(), expectedBeats, actualBeats);
			
			// the same as above
			// double percent = calcSimilarity(new CemgilEvaluation(), expectedBeats, actualBeats);
			// double percent = calcSimilaritySingleRow(new CemgilEvaluation(), expectedBeats, actualBeats);
			
			averageCemgil += percent;
			cemgilWriter.write(String.format("%.2f\r\n", percent));
		}
		
		average = average / countOfTestSongs;
		averageCemgil = averageCemgil / countOfTestSongs;
		
		fmeasureWriter.write(String.format("\r\n\r\n%.2f", average));
		fmeasureWriter.close();
		
		cemgilWriter.write(String.format("\r\n\r\n%.2f", averageCemgil));
		cemgilWriter.close();
		
		LOG.info("Done!");
	}

	// used to get max percent when comparing calculated sequence with ground-truth
	private static double calcSimilarityMax(IBeatAccuracyCalculator beatCalculator, 
			double[][] expected, double[] actual)
	{
		actual = RemoveBeatsWithinFiveSeconds(actual);
		
		double max = 0;
		for (double[] expectedRow : expected) {
			expectedRow = RemoveBeatsWithinFiveSeconds(expectedRow);
			
			max = Math.max(max, beatCalculator.getBeatAccuracy(actual, expectedRow));
		}
		
		return max;
	}

	// used to get average percent when comparing calculated sequence with ground-truth
	private static double calcSimilarity(IBeatAccuracyCalculator beatCalculator, 
			double[][] expected, double[] actual)
	{
		actual = RemoveBeatsWithinFiveSeconds(actual);
		
		double average = 0;
		for (double[] expectedRow : expected) {
			expectedRow = RemoveBeatsWithinFiveSeconds(expectedRow);
			
			// accumulate all values
			average += beatCalculator.getBeatAccuracy(actual, expectedRow);
		}
		average = average / expected.length;
		return average;
	}
	
	// used to get average percent when comparing calculated sequence with ground-truth
	// when alternative way is used
	private static double calcSimilaritySingleRow(IBeatAccuracyCalculator beatCalculator, 
			double[] expected, double[] actual)
	{
		actual = RemoveBeatsWithinFiveSeconds(actual);
		expected = RemoveBeatsWithinFiveSeconds(expected);
		
		return beatCalculator.getBeatAccuracy(actual, expected);
	}

	// some of sequences can begin only after 5 seconds,
	// so mirex community used to remove all peaks within this period of time
	private static double[] RemoveBeatsWithinFiveSeconds(double[] expected) {
		int fiveSeconds = 5;
		
		int i = 0;
		while (i < expected.length && expected[i] < fiveSeconds) {
			i++;
		}
		
		double[] result = new double[expected.length - i];
		for (int j = 0; j < result.length; j++) {
			result[j] = expected[j + i];
		}
		
		return result;
	}

}
