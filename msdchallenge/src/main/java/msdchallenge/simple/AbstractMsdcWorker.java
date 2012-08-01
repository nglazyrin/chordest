package msdchallenge.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import msdchallenge.input.provider.TracksProvider;
import msdchallenge.input.reader.ListeningsFileReader.IListeningProcessor;
import msdchallenge.model.Listening;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMsdcWorker implements IListeningProcessor {

	static final Logger LOG = LoggerFactory.getLogger(AbstractMsdcWorker.class);

	protected static Map<String, Integer> tracks = new HashMap<String, Integer>(Constants.TOTAL_TRACKS);

	protected String lastUser = null;
	protected List<Listening> lastUserListenings = new ArrayList<Listening>();

	protected int[][] trackIds;
	protected double[][] similarities;

	static {
		LOG.info("Initializing tracks...");
		tracks = new TracksProvider(Constants.KAGGLE_SONGS_FILE).getTracks();
	}

	public AbstractMsdcWorker() {
		trackIds = new int[Constants.PROCESS_TRACKS][];
		similarities = new double[Constants.PROCESS_TRACKS][];
	}

}
