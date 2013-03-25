package chordest.io.csv;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvSpectrumFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(CsvSpectrumFileReader.class);
	private final double[][] spectrum;
	
	public CsvSpectrumFileReader(File csv) {
		List<double[]> spectrumTemp = new LinkedList<double[]>();
		try (Scanner scanner = new Scanner(csv);) {
			scanner.useLocale(Locale.ENGLISH);
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				String[] strValues = line.split(",");
				double[] values = new double[strValues.length];
				for (int i = 0; i < strValues.length; i++) {
					values[i] = Double.parseDouble(strValues[i]);
				}
				spectrumTemp.add(values);
			}
			spectrum = new double[spectrumTemp.size()][];
			for (int i = 0; i < spectrumTemp.size(); i++) {
				spectrum[i] = spectrumTemp.get(i);
			}
			LOG.info("Spectrum values were read from " + csv.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (InputMismatchException e) {
			throw new IllegalArgumentException(e);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public double[][] getSpectrum() {
		return spectrum;
	}

}
