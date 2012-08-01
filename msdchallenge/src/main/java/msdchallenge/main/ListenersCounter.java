package msdchallenge.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import msdchallenge.input.ListeningsFileReader;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenersCounter extends AbstractMsdcWorker {

	private static final Logger LOG = LoggerFactory.getLogger(ListenersCounter.class);

	private int lowerBound;
	private int upperBound;

	private int[][] listenerCounts;

	public static void main(String[] args) {
		for (int i = 0; i < TOTAL_TRACKS / PROCESS_TRACKS; i ++) {
			int lower = i * PROCESS_TRACKS;
			int upper = (i + 1) * PROCESS_TRACKS;
			LOG.info("Processing tracks " + lower + " - " + upper);
			ListenersCounter lc = new ListenersCounter(lower, upper);
			lc.countListeners();
			lc.correctSimilarities();
			lc = null;
			LOG.info(upper + " tracks done");
		}
		LOG.info("finished");
	}

	private ListenersCounter(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		
		String tracksFileName = DATA_DIR + "trackIds" + lowerBound + ".bin";
		trackIds = deserialize(tracksFileName);
		
		listenerCounts = new int[PROCESS_TRACKS][];
		for (int i = 0; i < PROCESS_TRACKS; i++) {
			listenerCounts[i] = new int[trackIds[i].length];
		}
	}

	public void countListeners() {
		File allListenings = new File(TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		processUserListenings(lastUserListenings);
		
	}

	public void correctSimilarities() {
		String similaritiesFileName = DATA_DIR + "similarities" + lowerBound + ".bin";
		similarities = deserialize(similaritiesFileName);
		for (int i = 0; i < similarities.length; i++) {
			for (int j = 0; j < similarities[i].length; j++) {
				similarities[i][j] /= listenerCounts[i][j];
			}
		}
		serialize(similaritiesFileName, similarities);
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
			processUserListenings(lastUserListenings);
//			if (++usersProcessed % 100000 == 0) {
//				LOG.info(usersProcessed + " users processed");
//			}
			lastUser = listening.userId;
			lastUserListenings = new ArrayList<Listening>();
			lastUserListenings.add(listening);
		}
	}

	private void processUserListenings(List<Listening> listenings) {
		int length = listenings.size();
		for (int i = 0; i < length; i++) {
			Listening l1 = listenings.get(i);
			int t1 = tracks.get(l1.trackId);
			if (lowerBound <= t1 && t1 < upperBound) {
				int[] l1Tracks = trackIds[t1 - lowerBound];
				for (int j = 0; j < length; j++) {
					if (j == i) { continue; }
					int t2 = tracks.get(listenings.get(j).trackId);
					int index = ArrayUtils.indexOf(l1Tracks, t2);
					if (index >= 0) {
						listenerCounts[t1 - lowerBound][index] += 1;
					}
				}
			}
		}
	}

}
