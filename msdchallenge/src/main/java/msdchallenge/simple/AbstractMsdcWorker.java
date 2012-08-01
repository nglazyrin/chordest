package msdchallenge.simple;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import msdchallenge.input.ListeningsFileReader.IListeningProcessor;
import msdchallenge.input.SongNumbersFileReader;
import msdchallenge.input.SongNumbersFileReader.ISongNumberProcessor;
import msdchallenge.model.Listening;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMsdcWorker implements IListeningProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractMsdcWorker.class);

	public static final String DATA_DIR = "data" + File.separator;
	protected static final String KAGGLE_DIR = "kaggle" + File.separator;
	protected static final String KAGGLE_SONGS_FILE = KAGGLE_DIR + "kaggle_songs.txt";
	public static final String TRAIN_TRIPLETS_FILE = KAGGLE_DIR + "train_triplets.txt";
	public static final String TEST_TRIPLETS_FILE = KAGGLE_DIR + "kaggle_visible_evaluation_triplets.txt";

	public static final int TOTAL_TRACKS = 400000;

	public static final int TOTAL_USERS = 1129318;
	public static final int KAGGLE_USERS = 110000;

	public static final int PROCESS_TRACKS = 5000;

	public static final int MEANINGFUL_TRACKS = 200;

	protected static Map<String, Integer> tracks = new HashMap<String, Integer>(TOTAL_TRACKS);

	protected String lastUser = null;
	protected List<Listening> lastUserListenings = new ArrayList<Listening>();

	protected int[][] trackIds;
	protected double[][] similarities;

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

	public AbstractMsdcWorker() {
		trackIds = new int[PROCESS_TRACKS][];
		similarities = new double[PROCESS_TRACKS][];
	}

	public static void serialize(String fileName, Object data) {
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

	@SuppressWarnings("unchecked")
	public static <V> V deserialize(String fileName) {
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(fileName));
		    return (V) in.readObject();
//			LOG.info("Data was written to " + fileName);
		} catch (FileNotFoundException e) {
			LOG.error("Error when deserializing data", e);
		} catch (IOException e) {
			LOG.error("Error when deserializing data", e);
		} catch (ClassNotFoundException e) {
			LOG.error("Error when deserializing data", e);
		} finally {
			try {
				if (in != null) { in.close(); }
			} catch (IOException ignore) { }
		}
		return null;
	}

	protected <K> void addValue(Map<K, Double> map, K key, Double value) {
		if (map.containsKey(key)) {
			value += map.get(key);
		}
		map.put(key, value);
	}

	protected <K> void addValue(Map<K, Integer> map, K key, Integer value) {
		if (map.containsKey(key)) {
			value += map.get(key);
		}
		map.put(key, value);
	}

}
