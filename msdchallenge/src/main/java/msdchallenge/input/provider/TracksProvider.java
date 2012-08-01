package msdchallenge.input.provider;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import msdchallenge.input.reader.SongNumbersFileReader;
import msdchallenge.input.reader.SongNumbersFileReader.ISongNumberProcessor;

public class TracksProvider {

	private final Map<String, Integer> tracks = new HashMap<String, Integer>();

	public TracksProvider(String tracksFileName) {
		File songNumbers = new File(tracksFileName);
		SongNumbersFileReader.process(songNumbers, new ISongNumberProcessor() {
			@Override
			public void process(String trackId, int number) {
				tracks.put(trackId, number);
			}
		});
	}

	public Map<String, Integer> getTracks() {
		return tracks;
	}

}
