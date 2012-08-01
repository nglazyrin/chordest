package msdchallenge.input.reader;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try {
			LOG.info("Processing " + file.getName());
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis, 1048576);
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
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public interface IListeningProcessor {
		public void process(Listening listening);
	}

}
