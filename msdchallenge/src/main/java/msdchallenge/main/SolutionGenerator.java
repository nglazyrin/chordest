package msdchallenge.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import msdchallenge.input.reader.ListeningsFileReader;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;
import msdchallenge.simple.Constants;
import msdchallenge.simple.IoUtil;
import msdchallenge.simple.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolutionGenerator extends AbstractMsdcWorker {

	private static final Logger LOG = LoggerFactory.getLogger(SolutionGenerator.class);

	private static final String RESULT_DIR = "result" + File.separator;
	private static final String RESULT_FILE = RESULT_DIR + "result.txt";

	private FileWriter writer;

	private int usersProcessed = 0;

	public static void main(String[] args) {
		SolutionGenerator sg = new SolutionGenerator();
		sg.findMatchingSongs();
	}

	private SolutionGenerator() {
		super();
		trackIds = new int[Constants.TOTAL_TRACKS][];
		similarities = new double[Constants.TOTAL_TRACKS][];
		readData();
		try {
			writer = new FileWriter(RESULT_FILE);
		} catch (IOException e) {
			LOG.error("Could not open result file", e);
		}
	}

	private void readData() {
		for (int i = 0; i < 78; i++) {
			int number = i * Constants.PROCESS_TRACKS;
			String tracksFileName = Constants.DATA_DIR + "trackIds" + number + ".bin";
			String similaritiesFileName = Constants.DATA_DIR + "similarities" + number + ".bin";
			int[][] trackIdsLocal = IoUtil.deserialize(tracksFileName);
			double[][] similaritiesLocal = IoUtil.deserialize(similaritiesFileName);
			for (int j = 0; j < Constants.PROCESS_TRACKS; j++) {
				trackIds[j + number] = trackIdsLocal[j];
				similarities[j + number] = similaritiesLocal[j];
			}
		}
	}

	public void findMatchingSongs() {
		File allListenings = new File(Constants.TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		findSongsByListenings(lastUserListenings);
		
		closeWriter();
	}

	private void closeWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			LOG.error("Error closing result file", e);
		}
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
				MapUtil.addValue(candidates, key, value);
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
