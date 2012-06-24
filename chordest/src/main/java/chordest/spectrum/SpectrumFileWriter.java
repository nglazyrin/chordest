package chordest.spectrum;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SpectrumFileWriter {

	private static final Logger LOG = LoggerFactory.getLogger(SpectrumFileWriter.class);

	public static void write(String spectrumFilePath, SpectrumData data) {
		File file = new File(spectrumFilePath);
		ObjectOutput out =  null;
		try {
			FileUtils.forceMkdir(file.getParentFile());
			out = new ObjectOutputStream(new FileOutputStream(spectrumFilePath));
		    out.writeObject(data);
			LOG.info("Spectrum was written to " + spectrumFilePath);
		} catch (FileNotFoundException e) {
			LOG.error("Error when serializing spectrum data", e);
		} catch (IOException e) {
			LOG.error("Error when serializing spectrum data", e);
		} finally {
			try {
				if (out != null) { out.close(); }
			} catch (IOException ignore) { }
		}
	}

}
