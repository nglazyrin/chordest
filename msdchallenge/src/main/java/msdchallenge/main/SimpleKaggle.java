package msdchallenge.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import msdchallenge.input.ListeningsFileReader;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;
import msdchallenge.simple.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleKaggle extends AbstractMsdcWorker{

	private static final Logger LOG = LoggerFactory.getLogger(SimpleKaggle.class);

//	private int usersProcessed = 0;

	private int lowerBound;
	private int upperBound;

	private HashMap<Integer, Double>[] similarTracks;
	private HashMap<Integer, Integer>[] listenerCounts;

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

	@SuppressWarnings("unchecked")
	private SimpleKaggle(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		listenerCounts = new HashMap[PROCESS_TRACKS];
		similarTracks = new HashMap[PROCESS_TRACKS];
		for (int i = 0; i < PROCESS_TRACKS; i++) {
			listenerCounts[i] = new HashMap<Integer, Integer>(PROCESS_TRACKS);
			similarTracks[i] = new HashMap<Integer, Double>(PROCESS_TRACKS);
		}
	}

	public void fillSimilarities() {
		File allListenings = new File(TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		calculateSimilarity(lastUserListenings); // call manually for the last user's listenings
		
		File testListenings = new File(TEST_TRIPLETS_FILE);
		ListeningsFileReader.process(testListenings, this);
		calculateSimilarity(lastUserListenings); // call manually for the last user's listenings
	}

	private void cleanupSimilarity() {
//		int maxSize = 0;
		
		for (int i = 0; i < PROCESS_TRACKS; i++) {
			HashMap<Integer, Double> tracks = similarTracks[i];
			List<Entry<Integer, Double>> sorted = MapUtil.sortMapByValue(tracks, false);
			similarTracks[i] = null;
			int[] trackIdsI = new int[MEANINGFUL_TRACKS];
			double[] similaritiesI = new double[MEANINGFUL_TRACKS];
//			if (sorted.size() > maxSize) { maxSize = sorted.size(); }
			int limit = Math.min(MEANINGFUL_TRACKS, sorted.size());
			for (int j = 0; j < limit; j++) {
				Entry<Integer, Double> entry = sorted.get(j);
				trackIdsI[j] = entry.getKey();
				similaritiesI[j] = entry.getValue() / listenerCounts[i].get(entry.getKey()); // XXX normalization
			}
			trackIds[i] = trackIdsI;
			similarities[i] = similaritiesI;
		}
		similarTracks = null;
		
//		LOG.info("Maximum similarities list size: " + maxSize);
	}

	public void serializeSimilarities() {
		String similarTracksFileName = DATA_DIR + "trackIds" + lowerBound + ".bin";
		String similaritiesFileName = DATA_DIR + "similarities" + lowerBound + ".bin";
		
		serialize(similarTracksFileName, trackIds);
		serialize(similaritiesFileName, similarities);
		
		this.similarities = null;
		this.trackIds = null;
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
				HashMap<Integer, Integer> l1Counts = listenerCounts[t1 - lowerBound];
				for (int j = 0; j < length; j++) {
					if (j == i) { continue; }
					Listening l2 = listenings.get(j);
					Integer t2 = tracks.get(l2.trackId);
					double value = c1DivCSquared * l2.count;
					int count = 1;
					
					if (l1Tracks.containsKey(t2)) {
						value += l1Tracks.get(t2);
						count += l1Counts.get(t2);
					}
					l1Tracks.put(t2, value);
					l1Counts.put(t2, count);
				}
			}
		}
	}

}
