package chordest.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TracklistCreator {

	private static final Logger LOG = LoggerFactory.getLogger(TracklistCreator.class);

	public static List<String> createTracklist(File root, String prefix) {
		return createTracklist(root, prefix, PathConstants.EXT_LAB);
	}

	public static List<String> createTracklist(File root, String prefix, final String extension) {
		List<String> result = new LinkedList<String>();
		if (root.exists() && root.isDirectory()) {
			File[] labFiles = root.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (name.endsWith(extension) && !dir.getAbsolutePath().contains(".svn")) {
						return true;
					}
					return false;
			}});
			File[] subDirs = root.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				} });
			for (File labFile : labFiles) {
				result.add(prefix + labFile.getName());
			}
			for (File subDir : subDirs) {
				result.addAll(createTracklist(
						subDir, prefix + subDir.getName() + PathConstants.SEP));
			}
		}
		return result;
	}

	public static List<String> readTrackList(String fileName) {
		List<String> result = new LinkedList<String>();
		Scanner scanner = null;
		FileInputStream fis = null;
		try {
			LOG.info("Processing " + fileName);
			fis = new FileInputStream(fileName);
			scanner = new Scanner(fis);
			
			int total = 0;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				result.add(line);
				total++;
			}
			LOG.info(total + " file names have been read from " + fileName);
		} catch (FileNotFoundException e) {
			LOG.error("File not found: " + fileName, e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					LOG.error("Error when closing " + fileName, e);
				}
			}
		}
		return result;
	}

}
