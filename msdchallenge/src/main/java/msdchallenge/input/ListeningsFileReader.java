package msdchallenge.input;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

import msdchallenge.model.Listening;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ListeningsFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(ListeningsFileReader.class);

	public static void process(File file, IListeningProcessor processor) {
		if (file == null) {
			throw new NullPointerException();
		}
		if (processor == null) {
			throw new NullPointerException();
		}
		Scanner scanner = null;
		try {
			LOG.info("Processing " + file.getName());
			scanner = new Scanner(file);
			scanner.useLocale(Locale.ENGLISH);
			String userId, trackId;
			int count, total = 0;
			while (scanner.hasNext()) {
				userId = scanner.next();
				trackId = scanner.next();
				count = scanner.nextInt();
				processor.process(new Listening(userId, trackId, count));
				total++;
			}
			LOG.info(total + " listenings have been read from " + file.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	public interface IListeningProcessor {
		public void process(Listening listening);
	}

}
