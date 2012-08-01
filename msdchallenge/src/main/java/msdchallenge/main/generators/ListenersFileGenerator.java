package msdchallenge.main.generators;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import msdchallenge.input.provider.TracksProvider;
import msdchallenge.input.provider.UsersProvider;
import msdchallenge.input.reader.ListeningsFileReader;
import msdchallenge.input.reader.ListeningsFileReader.IListeningProcessor;
import msdchallenge.model.Listening;
import msdchallenge.simple.Constants;
import msdchallenge.simple.IoUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenersFileGenerator implements IListeningProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ListenersFileGenerator.class);

	private Map<String, Integer> tracksMap;
	private HashMap<String, Integer> usersMap;
	private int[][] tracksByUser;
	private short[][] countsByUser;

	private String lastUser = "";
	private List<Listening> lastUserListenings = new ArrayList<Listening>();

	public static void main(String[] args) {
		ListenersFileGenerator lfg = new ListenersFileGenerator();
		lfg.readListenings();
		lfg.serializeData();
	}

	private ListenersFileGenerator() {
		tracksMap = new TracksProvider(Constants.KAGGLE_SONGS_FILE).getTracks();
		usersMap = new UsersProvider(Constants.ALL_USERS_FILE).getUsers();
		tracksByUser = new int[usersMap.size()][];
		countsByUser = new short[usersMap.size()][];
	}

	private void readListenings() {
		File trainListenings = new File(Constants.TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(trainListenings, this);
		convertListenings(lastUserListenings, lastUser);
		
		File testListenings = new File(Constants.TEST_TRIPLETS_FILE);
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
		tracksByUser[userIndex] = localTracks;
		countsByUser[userIndex] = localCounts;
	}

	private void serializeData() {
		String tracksFileName = Constants.TRACKS_BY_USER_FILE;
		String countsFileName = Constants.COUNTS_BY_USER_FILE;
		IoUtil.serialize(tracksFileName, tracksByUser);
		IoUtil.serialize(countsFileName, countsByUser);
		LOG.info(tracksByUser.length + " users' listenings have been written to " + Constants.TRACKS_BY_USER_FILE);
		LOG.info(tracksByUser.length + " users' listening counts have been written to " + Constants.COUNTS_BY_USER_FILE);
	}

}
