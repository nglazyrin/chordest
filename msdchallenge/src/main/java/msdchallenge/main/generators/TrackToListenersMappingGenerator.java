package msdchallenge.main.generators;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import msdchallenge.input.ListeningsFileReader;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;
import msdchallenge.simple.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackToListenersMappingGenerator extends AbstractMsdcWorker {

	private static final Logger LOG = LoggerFactory.getLogger(TrackToListenersMappingGenerator.class);

//	private int usersProcessed = 0;

	private int lowerBound;
	private int upperBound;

	private String[][] listeners;

	private HashMap<String, Integer>[] trackToListeners;

	public static void main(String[] args) {
		for (int i = 0; i < TOTAL_TRACKS / PROCESS_TRACKS; i ++) {
			int lower = i * PROCESS_TRACKS;
			int upper = (i + 1) * PROCESS_TRACKS;
			LOG.info("Processing tracks " + lower + " - " + upper);
			TrackToListenersMappingGenerator ttlmg = new TrackToListenersMappingGenerator(lower, upper);
			ttlmg.findListeners();
			ttlmg.sortAndSerialize();
			ttlmg = null;
			LOG.info(upper + " tracks done");
		}
		LOG.info("finished");
	}

	@SuppressWarnings("unchecked")
	private TrackToListenersMappingGenerator(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		trackToListeners = new HashMap[PROCESS_TRACKS];
		listeners = new String[PROCESS_TRACKS][];
		for (int i = 0; i < PROCESS_TRACKS; i++) {
			trackToListeners[i] = new HashMap<String, Integer>(MEANINGFUL_TRACKS);
		}
	}

	public void findListeners() {
		File allListenings = new File(TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		
		File testListenings = new File(TEST_TRIPLETS_FILE);
		ListeningsFileReader.process(testListenings, this);
	}

	private void sortAndSerialize() {
		for (int i = 0; i < PROCESS_TRACKS; i++) {
			List<Entry<String, Integer>> sorted = MapUtil.sortMapByValue(trackToListeners[i], false);
			trackToListeners[i] = null;
			int max = Math.min(100, sorted.size());
			String[] array = new String[max];
			for (int j = 0; j < max; j++) {
				array[j] = sorted.get(j).getKey();
			}
			listeners[i] = array;
		}
		
		String listenersFileName = DATA_DIR + "listeners" + lowerBound + ".bin";
		serialize(listenersFileName, listeners);
	}

	@Override
	public void process(Listening listening) {
		int track = tracks.get(listening.trackId);
		if (lowerBound <= track && track < upperBound) {
			trackToListeners[track - lowerBound].put(listening.userId, listening.count);
		}
	}

}
