package msdchallenge.simple;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoUtil {

	private static final Logger LOG = LoggerFactory.getLogger(IoUtil.class);

	public static void serialize(String fileName, Object data) {
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(fileName));
		    out.writeObject(data);
//			LOG.info("Data was written to " + fileName);
		} catch (FileNotFoundException e) {
			AbstractMsdcWorker.LOG.error("Error when serializing data", e);
		} catch (IOException e) {
			AbstractMsdcWorker.LOG.error("Error when serializing data", e);
		} finally {
			try {
				if (out != null) { out.close(); }
			} catch (IOException ignore) { }
		}
	}

	@SuppressWarnings("unchecked")
	public static <V> V deserialize(String fileName) {
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(fileName));
		    return (V) in.readObject();
//			LOG.info("Data was written to " + fileName);
		} catch (FileNotFoundException e) {
			AbstractMsdcWorker.LOG.error("Error when deserializing data", e);
		} catch (IOException e) {
			AbstractMsdcWorker.LOG.error("Error when deserializing data", e);
		} catch (ClassNotFoundException e) {
			AbstractMsdcWorker.LOG.error("Error when deserializing data", e);
		} finally {
			try {
				if (in != null) { in.close(); }
			} catch (IOException ignore) { }
		}
		return null;
	}

	public static void closeWriter(FileWriter writer) {
		try {
			writer.close();
		} catch (IOException e) {
			LOG.error("Error closing result file", e);
		}
	}

}
