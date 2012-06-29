package chordest.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;


public class TracklistCreator {

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

}
