package msdchallenge.input.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SongNumbersFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(SongNumbersFileReader.class);

	public static void process(File file, ISongNumberProcessor processor) {
		if (file == null) {
			throw new NullPointerException();
		}
		if (processor == null) {
			throw new NullPointerException();
		}
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
			scanner.useLocale(Locale.ENGLISH);
			String trackId;
			int number, total = 0;
			while (scanner.hasNext()) {
				trackId = scanner.next();
				number = scanner.nextInt();
				processor.process(trackId, number);
				total++;
			}
			LOG.info(total + " song numbers have been read from " + file.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	public interface ISongNumberProcessor {
		public void process(String trackId, int number);
	}

}
