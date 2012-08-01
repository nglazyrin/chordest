package msdchallenge.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import msdchallenge.input.provider.ListenersProvider;
import msdchallenge.input.provider.ListeningsProvider;
import msdchallenge.input.provider.UsersProvider;
import msdchallenge.input.reader.ListeningsFileReader;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;
import msdchallenge.simple.Constants;
import msdchallenge.simple.IoUtil;
import msdchallenge.simple.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolutionGenerator2 extends AbstractMsdcWorker {

	private static final Logger LOG = LoggerFactory.getLogger(SolutionGenerator2.class);

	private static final String RESULT_DIR = "result" + File.separator;
	private static final String RESULT_FILE = RESULT_DIR + "result.txt";

	private FileWriter writer;

	private int usersProcessed = 0;

	private String[][] listeners;
	private int[][] tracksByUser;
	private short[][] countsByUser;

	private HashMap<String, Integer> users;

	public static void main(String[] args) {
		SolutionGenerator2 sg2 = new SolutionGenerator2();
		sg2.findMatchingSongs();
	}

	private SolutionGenerator2() {
		super();
		users = new UsersProvider(Constants.ALL_USERS_FILE).getUsers();
		ListeningsProvider lp = new ListeningsProvider();
		tracksByUser = lp.getTracks();
		countsByUser = lp.getCounts();
		listeners = new ListenersProvider().getListeners();
		try {
			writer = new FileWriter(RESULT_FILE);
		} catch (IOException e) {
			LOG.error("Could not open result file", e);
		}
	}

	public void findMatchingSongs() {
		File allListenings = new File(Constants.TEST_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, this);
		findSongsByListenings(lastUserListenings);
		
		IoUtil.closeWriter(writer);
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
		// get users who listen to the same tracks
		HashMap<String, Integer> similarUsers = new HashMap<String, Integer>();
		for (Listening l : listenings) {
			int track = tracks.get(l.trackId);
			String[] usersWhoListen = listeners[track];
			for (String user : usersWhoListen) {
				MapUtil.addValue(similarUsers, user, 1);
			}
		}
		
		// for each user: get listenings of this user
		// 		calculate additions to the ranks of tracks
		HashMap<Integer, Double> ranks = new HashMap<Integer, Double>(Constants.MEANINGFUL_TRACKS);
		for (Entry<String, Integer> entry : similarUsers.entrySet()) {
			if (entry.getValue() <= 1) {
				continue; // skip almost dissimilar users
			}
			int userIndex = users.get(entry.getKey());
			short[] countsForUser = countsByUser[userIndex];
			int[] tracksForUser = tracksByUser[userIndex];
			double c = 0;
			for (int i = 0; i < countsForUser.length; i++) {
				c += countsForUser[i];
			}
			double wc = entry.getValue() / c;
			
			for (int i = 0; i < countsForUser.length; i++) {
				double value = wc * countsForUser[i];
				MapUtil.addValue(ranks, tracksForUser[i], value);
			}
		}
		
		// remove tracks already listened by the user
		for (Listening l : listenings) {
			Integer track = tracks.get(l.trackId);
			ranks.remove(track);
		}
		ranks.remove(0);
		
		// add dummy entries if size < 500
		if (ranks.size() < 500) {
			int dummy = 1;
			while (ranks.size() < 500) {
				if (! ranks.containsKey(dummy)) {
					ranks.put(dummy, 0.0);
				}
				dummy++;
			}
		}
		
		// sort tracks by rank, select top-500 and output
		List<Entry<Integer, Double>> sorted = MapUtil.sortMapByValue(ranks, false);
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
