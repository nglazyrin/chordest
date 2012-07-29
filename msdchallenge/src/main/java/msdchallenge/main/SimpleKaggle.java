package msdchallenge.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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

public class SimpleKaggle implements IListeningProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(SimpleKaggle.class);

	private static final String KAGGLE_DIR = "kaggle" + File.separator;
	private static final String KAGGLE_SONGS_FILE = KAGGLE_DIR + "kaggle_songs.txt";
	private static final String TRAIN_TRIPLETS_FILE = KAGGLE_DIR + "train_triplets.txt";

	private static final int TOTAL_TRACKS = 1000000;

	private static final int PROCESS_TRACKS = 5000;

	private static final int MEANINGFUL_TRACKS = 100;

	private static Map<String, Integer> tracks = new HashMap<String, Integer>(TOTAL_TRACKS);

	private HashMap<Integer, Double>[] similarTracks;

	private int[][] resultTrackIds;
	private double[][] resultSimilarities;

	private String lastUser = null;
	private List<Listening> lastUserListenings = new ArrayList<Listening>();
//	private int usersProcessed = 0;

	private int lowerBound;
	private int upperBound;

	static {
		LOG.info("Initializing tracks...");
		File songNumbers = new File(KAGGLE_SONGS_FILE);
		SongNumbersFileReader.process(songNumbers, new ISongNumberProcessor() {
			@Override
			public void process(String trackId, int number) {
				tracks.put(trackId, number);
			}
		});
	}

	public static void main(String[] args) {
		for (int i = 0; i < TOTAL_TRACKS / PROCESS_TRACKS; i ++) {
			int lower = i * PROCESS_TRACKS;
			int upper = (i + 1) * PROCESS_TRACKS;
			LOG.info("Processing tracks " + lower + " - " + upper);
			SimpleKaggle sk = new SimpleKaggle(lower, upper);
			sk.fillSimilarities();
			sk.cleanupSimilarity();
			sk.serializeSimilarities();
			sk = null;
			LOG.info(upper + " tracks done");
		}
		LOG.info("finished");
	}

	private SimpleKaggle(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		similarTracks = new HashMap[PROCESS_TRACKS];
		initializeArrays();
	}

	private void initializeArrays() {
		for (int i = 0; i < PROCESS_TRACKS; i++) {
			similarTracks[i] = new HashMap<Integer, Double>(PROCESS_TRACKS);
		}
		resultTrackIds = new int[PROCESS_TRACKS][];
		resultSimilarities = new double[PROCESS_TRACKS][];
	}

	public void fillSimilarities() {
		File allListenings = new File(TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		calculateSimilarity(lastUserListenings); // call manually for the last user's listenings
	}

	private void cleanupSimilarity() {
//		int maxSize = 0;
		
		for (int i = 0; i < PROCESS_TRACKS; i++) {
			HashMap<Integer, Double> tracks = similarTracks[i];
			List<Entry<Integer, Double>> sorted = MapUtil.sortMapByValue(tracks, false);
			similarTracks[i] = null;
			int[] trackIds = new int[MEANINGFUL_TRACKS];
			double[] similarities = new double[MEANINGFUL_TRACKS];
//			if (sorted.size() > maxSize) { maxSize = sorted.size(); }
			int limit = Math.min(MEANINGFUL_TRACKS, sorted.size());
			for (int j = 0; j < limit; j++) {
				Entry<Integer, Double> entry = sorted.get(j);
				trackIds[j] = entry.getKey();
				similarities[j] = entry.getValue();
			}
			resultTrackIds[i] = trackIds;
			resultSimilarities[i] = similarities;
		}
		similarTracks = null;
		
//		LOG.info("Maximum similarities list size: " + maxSize);
	}

	public void serializeSimilarities() {
		String similarTracksFileName = "data" + File.separator + "trackIds" + lowerBound + ".bin";
		String similaritiesFileName = "data" + File.separator + "similarities" + lowerBound + ".bin";
		
		serialize(similarTracksFileName, resultTrackIds);
		serialize(similaritiesFileName, resultSimilarities);
		
		this.resultSimilarities = null;
		this.resultTrackIds = null;
	}

	private void serialize(String fileName, Object data) {
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(fileName));
		    out.writeObject(data);
//			LOG.info("Data was written to " + fileName);
		} catch (FileNotFoundException e) {
			LOG.error("Error when serializing data", e);
		} catch (IOException e) {
			LOG.error("Error when serializing data", e);
		} finally {
			try {
				if (out != null) { out.close(); }
			} catch (IOException ignore) { }
		}
	}

	@Override
	public void process(Listening listening) {
		if (listening.count <= 1) {
			return;
		}
		if (listening.userId.equals(lastUser)) {
			lastUserListenings.add(listening);
		} else if (lastUser == null) {
			lastUser = listening.userId;
			lastUserListenings.add(listening);
		} else {
			calculateSimilarity(lastUserListenings);
//			if (++usersProcessed % 100000 == 0) {
//				LOG.info(usersProcessed + " users processed");
//			}
			lastUser = listening.userId;
			lastUserListenings = new ArrayList<Listening>();
			lastUserListenings.add(listening);
		}
	}

	private void calculateSimilarity(List<Listening> listenings) {
		double c = 0;
		for (Listening l : listenings) {
			c += l.count;
		}
		double cSquared = c*c;
		int length = listenings.size();
		for (int i = 0; i < length; i++) {
			Listening l1 = listenings.get(i);
			Integer t1 = tracks.get(l1.trackId);
			if (lowerBound <= t1 && t1 < upperBound) {
				double c1DivCSquared = l1.count / cSquared;
				
				HashMap<Integer, Double> l1Tracks = similarTracks[t1 - lowerBound];
				for (int j = 0; j < length; j++) {
					if (j == i) { continue; }
					Listening l2 = listenings.get(j);
					Integer t2 = tracks.get(l2.trackId);
					double value = c1DivCSquared * l2.count;
					
					if (l1Tracks.containsKey(t2)) {
						value += l1Tracks.get(t2);
					}
					l1Tracks.put(t2, value);
				}
			}
		}
	}

}
