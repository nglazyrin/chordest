package msdchallenge.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import msdchallenge.input.ListeningsFileReader;
import msdchallenge.input.ListeningsFileReader.IListeningProcessor;
import msdchallenge.input.SongNumbersFileReader;
import msdchallenge.input.SongNumbersFileReader.ISongNumberProcessor;
import msdchallenge.model.Listening;
import msdchallenge.simple.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolutionGenerator implements IListeningProcessor, ISongNumberProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(SolutionGenerator.class);

	private static final String DATA_DIR = "data" + File.separator;
	private static final String KAGGLE_DIR = "kaggle" + File.separator;
	private static final String RESULT_DIR = "result" + File.separator;
	private static final String KAGGLE_SONGS_FILE = KAGGLE_DIR + "kaggle_songs.txt";
	private static final String VISIBLE_TRIPLETS_FILE = KAGGLE_DIR + "kaggle_visible_evaluation_triplets.txt";
	private static final String RESULT_FILE = RESULT_DIR + "result.txt";

	private static final int TOTAL_TRACKS = 1000000;

	private static final int PROCESS_TRACKS = 5000;

	private final Map<String, Integer> tracks;

	private int[][] trackIds;
	private double[][] similarities;

	private String lastUser = null;
	private List<Listening> lastUserListenings = new ArrayList<Listening>();

	private FileWriter writer;

	private int usersProcessed = 0;

	public static void main(String[] args) {
		SolutionGenerator sg = new SolutionGenerator();
		sg.findMatchingSongs();
	}

	private SolutionGenerator() {
		tracks = new HashMap<String, Integer>(TOTAL_TRACKS);
		trackIds = new int[TOTAL_TRACKS][];
		similarities = new double[TOTAL_TRACKS][];
		readData();
		try {
			writer = new FileWriter(RESULT_FILE);
		} catch (IOException e) {
			LOG.error("Could not open result file", e);
		}
	}

	private void readData() {
		for (int i = 0; i < 78; i++) {
			int number = i * PROCESS_TRACKS;
			String tracksFileName = DATA_DIR + "trackIds" + number + ".bin";
			String similaritiesFileName = DATA_DIR + "similarities" + number + ".bin";
			int[][] trackIdsLocal = deserialize(tracksFileName);
			double[][] similaritiesLocal = deserialize(similaritiesFileName);
			for (int j = 0; j < PROCESS_TRACKS; j++) {
				trackIds[j + number] = trackIdsLocal[j];
				similarities[j + number] = similaritiesLocal[j];
			}
		}
	}

	private <V> V deserialize(String fileName) {
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(fileName));
		    return (V) in.readObject();
//			LOG.info("Data was written to " + fileName);
		} catch (FileNotFoundException e) {
			LOG.error("Error when deserializing data", e);
		} catch (IOException e) {
			LOG.error("Error when deserializing data", e);
		} catch (ClassNotFoundException e) {
			LOG.error("Error when deserializing data", e);
		} finally {
			try {
				if (in != null) { in.close(); }
			} catch (IOException ignore) { }
		}
		return null;
	}

	public void findMatchingSongs() {
		File songNumbers = new File(KAGGLE_SONGS_FILE);
		SongNumbersFileReader.process(songNumbers, this);

		File allListenings = new File(VISIBLE_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		findSongsByListenings(lastUserListenings);
		
		try {
			writer.close();
		} catch (IOException e) {
			LOG.error("Error closing result file", e);
		}
	}

	@Override
	public void process(String trackId, int number) {
		tracks.put(trackId, number);
	}

	@Override
	public void process(Listening listening) {
		if (listening.userId.equals(lastUser)) {
			lastUserListenings.add(listening);
		} else if (lastUser == null) {
			lastUser = listening.userId;
			lastUserListenings.add(listening);
		} else {
			findSongsByListenings(lastUserListenings);
			if (++usersProcessed % 1000 == 0) {
				LOG.info(usersProcessed + " users processed");
			}
			lastUser = listening.userId;
			lastUserListenings = new ArrayList<Listening>();
			lastUserListenings.add(listening);
		}
	}

	private void findSongsByListenings(List<Listening> listenings) {
		double c = 0;
		for (Listening l : listenings) {
			c += l.count;
		}
		double cSquared = c*c;
		
		// find most similar tracks
		Map<Integer, Double> candidates = new HashMap<Integer, Double>();
		for (Listening listening : listenings) {
			double cl = listening.count / cSquared;
			
			int track = tracks.get(listening.trackId);
			int[] similarTracks = trackIds[track];
			double[] similarSimilarities = similarities[track];
			for (int i = 0; i < similarTracks.length; i++) {
				double value = cl * similarSimilarities[i];
				Integer key = similarTracks[i];
				if (candidates.containsKey(key)) {
					value += candidates.get(key);
				}
				candidates.put(key, value);
			}
		}
		
		// remove already known songs
		for (Listening l : listenings) {
			int track = tracks.get(l.trackId);
			candidates.remove(track);
		}
		candidates.remove(0); // just in case
		
		// add dummy entries if size < 500
		if (candidates.size() < 500) {
			int dummy = 1;
			while (candidates.size() < 500) {
				if (! candidates.containsKey(dummy)) {
					candidates.put(dummy, 0.0);
				}
				dummy++;
			}
		}
		
		List<Entry<Integer, Double>> sorted = MapUtil.sortMapByValue(candidates, false);
		writeResultLine(sorted);
	}

	private void writeResultLine(List<Entry<Integer, Double>> sorted) {
		for (int i = 0; i < 500; i++) {
			try {
				String value = sorted.get(i).getKey().toString();
				writer.write(sorted.get(i).getKey().toString());
				writer.write(" ");
			} catch (IOException e) {
				LOG.error("Error writing to result file", e);
			}
		}
		try {
			writer.write("\r\n");
		} catch (IOException e) {
			LOG.error("Error writing to result file", e);
		}
	}

}
