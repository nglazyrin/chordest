package msdchallenge.simple;

import java.io.File;

public class Constants {

	public static final String DATA_DIR = "data" + File.separator;
	public static final String KAGGLE_DIR = "kaggle" + File.separator;
	public static final String KAGGLE_SONGS_FILE = KAGGLE_DIR + "kaggle_songs.txt";
	public static final String TEST_TRIPLETS_FILE = KAGGLE_DIR + "kaggle_visible_evaluation_triplets.txt";
	public static final String TRAIN_TRIPLETS_FILE = KAGGLE_DIR + "train_triplets.txt";
	public static final String ALL_USERS_FILE = KAGGLE_DIR + "all_users.txt";
	public static final String COUNTS_BY_USER_FILE = DATA_DIR + "countsByUser.bin";
	public static final String TRACKS_BY_USER_FILE = DATA_DIR + "tracksByUser.bin";
	
	public static final int TOTAL_TRACKS = 400000;
	public static final int TOTAL_USERS = 1129318;
	public static final int KAGGLE_USERS = 110000;
	public static final int PROCESS_TRACKS = 5000;
	public static final int MEANINGFUL_TRACKS = 200;

	private Constants() { }

}
