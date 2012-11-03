package chordest.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWriter {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWriter.class);

	abstract public void writeTo(Writer writer) throws IOException;
	abstract public void appendTo(Writer writer) throws IOException;

	public void writeTo(File file) throws IOException {
		if (file == null) {
			return;
		}
		FileUtils.forceMkdir(file.getParentFile());
		FileWriter fw = null;
		String fileName = file.getAbsolutePath();
		try {
			fw = new FileWriter(file);
			writeTo(fw);
			LOG.debug("Result saved to " + fileName);
		} catch (IOException e) {
			LOG.error("Cannot write to " + fileName);
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				LOG.error("Cannot write to " + fileName);
				e.printStackTrace();
			}
		}
	}

	public void appendTo(File file) {
		if (file == null) {
			return;
		}
		FileWriter fw = null;
		String fileName = file.getName();
		try {
			fw = new FileWriter(file, true);
			appendTo(fw);
			LOG.debug("Result saved to " + fileName);
		} catch (IOException e) {
			LOG.error("Cannot write to " + fileName);
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				LOG.error("Cannot write to " + fileName);
				e.printStackTrace();
			}
		}
	}

}
