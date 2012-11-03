package chordest.spectrum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.io.spectrum.SpectrumFileReader;

public class FileSpectrumDataProvider implements ISpectrumDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(FileSpectrumDataProvider.class);

	private final SpectrumData spectrumData;

	public FileSpectrumDataProvider(String spectrumFileName) {
		spectrumData = SpectrumFileReader.read(spectrumFileName);
		if (spectrumData != null) {
			LOG.info("Spectrum was read from " + spectrumFileName);
		} else {
			LOG.error("Error when reading spectrum data from " + spectrumFileName);
		}
	}

	@Override
	public SpectrumData getSpectrumData() {
		return spectrumData;
	}

}
