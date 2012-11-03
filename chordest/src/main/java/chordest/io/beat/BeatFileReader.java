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

import chordest.io.lab.chordparser.TokenMgrError;


public class BeatFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(BeatFileReader.class);
	private final double[] timestamps;
	
	public BeatFileReader(File lab) {
		List<Double> timestampsTemp = new LinkedList<Double>();
		Scanner scanner = null;
		try {
			scanner = new Scanner(lab);
			scanner.useLocale(Locale.ENGLISH);
			double time = 0;
			while (scanner.hasNext()) {
				time = scanner.nextDouble();
				timestampsTemp.add(time);
			}
			timestamps = new double[timestampsTemp.size()];
			for (int i = 0; i < timestampsTemp.size(); i++) {
				timestamps[i] = timestampsTemp.get(i);
			}
			LOG.info("Beat times were read from " + lab.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (InputMismatchException e) {
			throw new IllegalArgumentException(e);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		} catch (TokenMgrError e) {
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

}
