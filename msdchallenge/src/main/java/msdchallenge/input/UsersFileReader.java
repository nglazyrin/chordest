package msdchallenge.input;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersFileReader {

	private static final Logger LOG = LoggerFactory.getLogger(UsersFileReader.class);

	public static void process(File file, IUserProcessor processor) {
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
			String userId;
			int total = 0;
			while (scanner.hasNext()) {
				userId = scanner.next();
				processor.process(userId);
				total++;
			}
			LOG.info(total + " users have been read from " + file.getAbsolutePath());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	public interface IUserProcessor {
		public void process(String kaggleUser);
	}

}
