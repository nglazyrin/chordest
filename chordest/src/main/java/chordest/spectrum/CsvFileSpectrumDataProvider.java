package chordest.spectrum;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.csv.CsvSpectrumFileReader;
import chordest.io.spectrum.SpectrumFileReader;

public class CsvFileSpectrumDataProvider implements ISpectrumDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(CsvFileSpectrumDataProvider.class);

	private final SpectrumData spectrumData;

	public CsvFileSpectrumDataProvider(String spectrumFileName, String csvFileName) {
		spectrumData = SpectrumFileReader.read(spectrumFileName);
		if (spectrumData != null) {
			LOG.info("Spectrum was read from " + spectrumFileName);
		} else {
			LOG.error("Error when reading spectrum data from " + spectrumFileName);
		}
		
		spectrumData.spectrum = new CsvSpectrumFileReader(new File(csvFileName)).getSpectrum();
	}

	@Override
	public SpectrumData getSpectrumData() {
		return spectrumData;
	}

}
