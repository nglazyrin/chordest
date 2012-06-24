package chordest.spectrum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SpectrumFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(SpectrumFileReader.class);

	public static SpectrumData read(String spectrumFilePath) {
		File file = null;
		if (spectrumFilePath != null) {
			file = new File(spectrumFilePath);
		}
		if (file != null && file.exists()) {
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(file));
			    return (SpectrumData) in.readObject();
			} catch (FileNotFoundException e) {
				LOG.error("Error when reading object from " + spectrumFilePath, e);
			} catch (IOException e) {
				LOG.error("Error when reading object from " + spectrumFilePath, e);
			} catch (ClassNotFoundException e) {
				LOG.error("Error when casting object to " + SpectrumData.class.getName(), e);
			} finally {
			    if (in != null) {
			    	try {
						in.close();
					} catch (IOException e) {
						LOG.error("Error when closing input stream", e);
					}
				}
			}
		}
		return null;
	}

}
