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

/**
 * Used together with FileBeatBarTimesProvider. Encapsulates low level
 * reading and parsing of the beat and bars file.
 * @author Nikolay
 *
 */
public class Beat2FileReader {

	private static final Logger LOG = LoggerFactory.getLogger(Beat2FileReader.class);
	private final double[] timestamps;
	private final int[] bars;
	
	public Beat2FileReader(File beat) {
		List<Double> timestampsTemp = new LinkedList<Double>();
		List<Integer> barsTemp = new LinkedList<Integer>();
		Scanner scanner = null;
		try {
			scanner = new Scanner(beat);
			scanner.useLocale(Locale.ENGLISH);
			double time = 0;
			int bar = 0;
			while (scanner.hasNext()) {
				String s = scanner.nextLine();
				String[] timeBar = s.split(": ");
				time = Double.parseDouble(timeBar[0]);
				timestampsTemp.add(time);
				bar = 0;
				if (timeBar.length > 1) {
					try {
						bar = Integer.parseInt(timeBar[1]);
					} catch (NumberFormatException ignore) { }
				}
				barsTemp.add(bar);
			}
			timestamps = new double[timestampsTemp.size()];
			bars = new int[barsTemp.size()];
			for (int i = 0; i < timestampsTemp.size(); i++) {
				timestamps[i] = timestampsTemp.get(i);
				bars[i] = barsTemp.get(i);
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

	public double[] getTimestamps() {
		return timestamps;
	}

	public int[] getBars() {
		return bars;
	}

}
