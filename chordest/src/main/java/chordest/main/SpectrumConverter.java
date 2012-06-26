package chordest.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chordest.spectrum.SpectrumData;
import chordest.spectrum.SpectrumFileWriter;
import chordest.util.PathConstants;
import chordest.util.TracklistCreator;

public class SpectrumConverter {

	private static final Logger LOG = LoggerFactory.getLogger(SpectrumConverter.class);
	private static final String SPECTRUM_DIR = "spectrum_tuning" + PathConstants.SEP;

	public static void main(String[] args) {
		List<String> tracklist = TracklistCreator.createTracklist(new File(PathConstants.LAB_DIR), "");
		for (final String labFileName : tracklist) {
			final String spectrumFileName = SPECTRUM_DIR + 
					labFileName.replace(PathConstants.EXT_LAB, PathConstants.EXT_BIN);
			similarity.spectrum.SpectrumData s = read(spectrumFileName);
			SpectrumFileWriter.write(spectrumFileName, s.toNewSpectrumData());
		}
	}

	public static similarity.spectrum.SpectrumData read(String spectrumFilePath) {
		File file = null;
		if (spectrumFilePath != null) {
			file = new File(spectrumFilePath);
		}
		if (file != null && file.exists()) {
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(file));
			    return (similarity.spectrum.SpectrumData) in.readObject();
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
