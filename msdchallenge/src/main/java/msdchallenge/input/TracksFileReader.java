package msdchallenge.input;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import msdchallenge.model.Track;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TracksFileReader {

	private static final Logger LOG = LoggerFactory
			.getLogger(TracksFileReader.class);

	public static void process(File file, ITrackProcessor processor) {
		if (file == null) {
			throw new NullPointerException();
		}
		if (processor == null) {
			throw new NullPointerException();
		}
		DataInputStream in = null;
		try {
			FileInputStream fstream = new FileInputStream(file);
			in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			String id1, trackId, artist, song;
			int total = 0;
			while ((line = br.readLine()) != null) {
				line = removeNonPrintable(line);
				String[] tokens = StringUtils.splitByWholeSeparator(line,"<SEP>");
				if (tokens.length == 4) {
					id1 = tokens[0];
					trackId = tokens[1];
					artist = tokens[2];
					song = tokens[3];
					processor.process(new Track(id1, trackId, artist, song));
					total++;
				}
			}
			LOG.info(total + " tracks have been read from "
					+ file.getAbsolutePath());
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (in != null) { try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} }
		}
	}

	private static String removeNonPrintable(String s) {
		char[] oldChars = new char[s.length()];
		s.getChars(0, s.length(), oldChars, 0);
		char[] newChars = new char[s.length()];
		int newLen = 0;
		for (int j = 0; j < s.length(); j++) {
			char ch = oldChars[j];
			if (ch >= ' ') {
				newChars[newLen] = ch;
				newLen++;
			}
		}
		return new String(newChars, 0, newLen);
	}

	public interface ITrackProcessor {
		public void process(Track track);
	}

}
