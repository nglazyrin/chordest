package chordest.io.chroma;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChromaFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(ChromaFileReader.class);
	private final double[] timestamps;
	private final double[][] chroma;

	public ChromaFileReader(File chromaFile) {
		List<Double> timestampsTemp = new LinkedList<Double>();
		List<double[]> chromaTemp = new LinkedList<double[]>();
		Scanner scanner = null;
		try {
			scanner = new Scanner(chromaFile);
			scanner.useLocale(Locale.ENGLISH);
			while (scanner.hasNext()) {
				String s = scanner.nextLine();
				String[] tokens = s.trim().split(" ");
				if (tokens.length != 13) {
					String msg = "Expected 13 tokens but found: " + tokens.length + " in string: '" + s + "'";
					LOG.error(msg);
					throw new RuntimeException(msg);
				}
				double time = Double.parseDouble(tokens[0].substring(0, tokens[0].length() - 1));
				double[] c = new double[12];
				for (int i = 0; i < 12; i++) {
					c[i] = Double.parseDouble(tokens[i+1]);
				}
				timestampsTemp.add(time);
				chromaTemp.add(c);
			}
			timestamps = new double[timestampsTemp.size()];
			chroma = new double[chromaTemp.size()][12];
			for (int i = 0; i < timestampsTemp.size(); i++) {
				timestamps[i] = timestampsTemp.get(i);
				chroma[i] = chromaTemp.get(i);
			}
			LOG.info("Chroma and timestamps were read from " + chromaFile.getAbsolutePath());
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

	public double[][] getChroma() {
		return chroma;
	}

	public double[] getTimestamps() {
		return timestamps;
	}

}
