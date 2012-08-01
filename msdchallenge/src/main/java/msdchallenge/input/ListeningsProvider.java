package msdchallenge.input;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import msdchallenge.input.ListeningsFileReader.IListeningProcessor;
import msdchallenge.model.Listening;
import msdchallenge.simple.AbstractMsdcWorker;

public class ListeningsProvider implements IListeningProcessor {

	private final Map<String, Integer> usersMap;
	private final Map<String, Integer> tracksMap;

	private final int[][] tracks = new int[AbstractMsdcWorker.TOTAL_USERS][];
	private final short[][] counts = new short[AbstractMsdcWorker.TOTAL_USERS][];

	private String lastUser = "";
	private List<Listening> lastUserListenings = new ArrayList<Listening>();

	public ListeningsProvider(Map<String, Integer> users, Map<String, Integer> tracks) {
		this.usersMap = users;
		this.tracksMap = tracks;
		
		File trainListenings = new File(AbstractMsdcWorker.TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(trainListenings, this);
		convertListenings(lastUserListenings, lastUser);
		
		File testListenings = new File(AbstractMsdcWorker.TEST_TRIPLETS_FILE);
		ListeningsFileReader.process(testListenings, this);
		convertListenings(lastUserListenings, lastUser);
	}

	@Override
	public void process(Listening listening) {
		if (listening.userId.equals(lastUser)) {
			lastUserListenings.add(listening);
		} else {
			if (! StringUtils.isEmpty(lastUser)) {
				convertListenings(lastUserListenings, lastUser);
			}
			lastUser = listening.userId;
			lastUserListenings = new ArrayList<Listening>();
			lastUserListenings.add(listening);
		}
	}

	private void convertListenings(List<Listening> listenings, String user) {
		int[] localTracks = new int[listenings.size()];
		short[] localCounts = new short[listenings.size()];
		for (int i = 0; i < localTracks.length; i++) {
			Listening l = listenings.get(i);
			localTracks[i] = tracksMap.get(l.trackId);
			localCounts[i] = (short) l.count;
		}
		int userIndex = usersMap.get(user);
		tracks[userIndex] = localTracks;
		counts[userIndex] = localCounts;
	}

	public int[][] getTracks() {
		return tracks;
	}

	public short[][] getCounts() {
		return counts;
	}

}
