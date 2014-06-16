package chordest.io.beat;

import chordest.beat.IBeatTimesProvider;
import chordest.beat.evaluation.IBeatAccuracyCalculator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// combine all sequences from musical experts in single row of beats
public class SingleRowBeatTimesFileReader implements IBeatTimesProvider {
	
	private static final Logger LOG = LoggerFactory.getLogger(BeatTimesFileReader.class);
	private List<Double> beatTimes = new LinkedList<Double>();
	
	public SingleRowBeatTimesFileReader(File beat) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(beat);
			scanner.useLocale(Locale.ENGLISH);
			double time;
			while (scanner.hasNext()) {
				String s = scanner.nextLine();
				String[] times = s.split("\t");
				for (String timeStr : times) {
					time = Double.parseDouble(timeStr);
					beatTimes.add(time);
				}
			}
			LOG.info("Beat times were read from " + beat.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (InputMismatchException e) {
			throw new IllegalArgumentException(e);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}
	
	@Override
	public double[] getBeatTimes() {
		
		Collections.sort(beatTimes);
		
		Double[] dArray = beatTimes.toArray(new Double[beatTimes.size()]);
		
		double[] result = new double[dArray.length];
		
		for (int i = 0; i < dArray.length; i++) {
			result[i] = dArray[i];
		}
		
		return result;
	}	
}