package msdchallenge.main.generators;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import msdchallenge.input.reader.ListeningsFileReader;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;
import msdchallenge.simple.Constants;
import msdchallenge.simple.IoUtil;
import msdchallenge.simple.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonlySimilarTracksFilesGenerator extends AbstractMsdcWorker{

	private static final Logger LOG = LoggerFactory.getLogger(CommonlySimilarTracksFilesGenerator.class);

//	private int usersProcessed = 0;

	private int lowerBound;
	private int upperBound;

	private HashMap<Integer, Double>[] similarTracks;
	private HashMap<Integer, Integer>[] listenerCounts;

	public static void main(String[] args) {
		for (int i = 0; i < Constants.TOTAL_TRACKS / Constants.PROCESS_TRACKS; i ++) {
			int lower = i * Constants.PROCESS_TRACKS;
			int upper = (i + 1) * Constants.PROCESS_TRACKS;
			LOG.info("Processing tracks " + lower + " - " + upper);
			CommonlySimilarTracksFilesGenerator sk = new CommonlySimilarTracksFilesGenerator(lower, upper);
			sk.fillSimilarities();
			sk.cleanupSimilarity();
			sk.serializeSimilarities();
			sk = null;
			LOG.info(upper + " tracks done");
		}
		LOG.info("finished");
	}

	@SuppressWarnings("unchecked")
	private CommonlySimilarTracksFilesGenerator(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		listenerCounts = new HashMap[Constants.PROCESS_TRACKS];
		similarTracks = new HashMap[Constants.PROCESS_TRACKS];
		for (int i = 0; i < Constants.PROCESS_TRACKS; i++) {
			listenerCounts[i] = new HashMap<Integer, Integer>(Constants.PROCESS_TRACKS);
			similarTracks[i] = new HashMap<Integer, Double>(Constants.PROCESS_TRACKS);
		}
	}

	public void fillSimilarities() {
		File allListenings = new File(Constants.TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		calculateSimilarity(lastUserListenings); // call manually for the last user's listenings
		
		File testListenings = new File(Constants.TEST_TRIPLETS_FILE);
		ListeningsFileReader.process(testListenings, this);
		calculateSimilarity(lastUserListenings); // call manually for the last user's listenings
	}

	private void cleanupSimilarity() {
//		int maxSize = 0;
		
		for (int i = 0; i < Constants.PROCESS_TRACKS; i++) {
			HashMap<Integer, Double> tracks = similarTracks[i];
			List<Entry<Integer, Double>> sorted = MapUtil.sortMapByValue(tracks, false);
			similarTracks[i] = null;
			int[] trackIdsI = new int[Constants.MEANINGFUL_TRACKS];
			double[] similaritiesI = new double[Constants.MEANINGFUL_TRACKS];
//			if (sorted.size() > maxSize) { maxSize = sorted.size(); }
			int limit = Math.min(Constants.MEANINGFUL_TRACKS, sorted.size());
			for (int j = 0; j < limit; j++) {
				Entry<Integer, Double> entry = sorted.get(j);
				trackIdsI[j] = entry.getKey();
				similaritiesI[j] = entry.getValue();
			}
			trackIds[i] = trackIdsI;
			similarities[i] = similaritiesI;
		}
		similarTracks = null;
		
//		LOG.info("Maximum similarities list size: " + maxSize);
	}

	public void serializeSimilarities() {
		String similarTracksFileName = Constants.DATA_DIR + "trackIds" + lowerBound + ".bin";
		String similaritiesFileName = Constants.DATA_DIR + "similarities" + lowerBound + ".bin";
		
		IoUtil.serialize(similarTracksFileName, trackIds);
		IoUtil.serialize(similaritiesFileName, similarities);
		
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
