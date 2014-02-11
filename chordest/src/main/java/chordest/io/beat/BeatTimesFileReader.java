package chordest.io.beat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeatTimesFileReader {
	private static final Logger LOG = LoggerFactory.getLogger(BeatTimesFileReader.class);
	private double[][] beatTimes;
	
	public BeatTimesFileReader(File beat) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(beat);
			scanner.useLocale(Locale.ENGLISH);
			List<List<Double>> series = new LinkedList<List<Double>>();
			double time;
			while (scanner.hasNext()) {
				List<Double> timestamps = new LinkedList<Double>();
				String s = scanner.nextLine();
				String[] times = s.split("\t");
				for (String timeStr : times) {
					time = Double.parseDouble(timeStr);
					timestamps.add(time);
				}
				series.add(timestamps);
			}
			
			// convert to double[][]
			beatTimes = new double[series.size()][];
			for (int i = 0; i < beatTimes.length; i++) {
				beatTimes[i] = new double[series.get(i).size()];
				for (int j = 0; j < beatTimes[i].length; j++) {
					beatTimes[i][j] = series.get(i).get(j);
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

	public double[][] getBeatTimes() {
		return beatTimes;
	}
}
