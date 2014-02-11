package chordest.beat.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import chordest.io.beat.BeatTimesFileReader;

public class EvaluationLauncher {

	public static void main(String[] args) throws IOException {
		String expFileName = "D:/USU/MIR/MIREX '11/beattrack_train_2006/train/train14.txt";
		File expFile = new File(expFileName);
		BeatTimesFileReader expReader = new BeatTimesFileReader(expFile);
		double[][] expectedBeats = expReader.getBeatTimes();
		
		String actFileName = "D:/USU/MIR/MIREX '11/beattrack_train_2006/train/output.txt";
		File actFile = new File(actFileName);
		BeatTimesFileReader actReader = new BeatTimesFileReader(actFile);
		double[] actualBeats = actReader.getBeatTimes()[0];
		
		String fmeasureFileName = "D:/USU/MIR/MIREX '11/beattrack_train_2006/train/fmeasure.txt";
		calcSimilarity(new FMeasureEvaluation(), expectedBeats, actualBeats, fmeasureFileName);
		
		// percent is too small. Either error either poor beat recognition
		String cemgilFileName = "D:/USU/MIR/MIREX '11/beattrack_train_2006/train/cemgil.txt";
		calcSimilarity(new CemgilEvaluation(), expectedBeats, actualBeats, cemgilFileName);		
	}
	
	private static void calcSimilarity(IBeatAccuracyCalculator beatCalculator, 
			double[][] expected, double[] actual, String fileName) throws IOException 
	{
		File resultFile = new File(fileName);
		Writer writer = new FileWriter(resultFile);
		
		for (double[] expectedRow : expected) {
			writer.write(String.format("%.2f\r\n", beatCalculator.getBeatAccuracy(actual, expectedRow)));
		}
		
		writer.close();
	}

}
