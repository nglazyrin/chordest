package msdchallenge.main;


import java.io.File;

import msdchallenge.input.InputProcessor;
import msdchallenge.input.ListeningsFileReader;
import msdchallenge.input.SongNumbersFileReader;
import msdchallenge.input.TracksFileReader;
import msdchallenge.input.UsersFileReader;
import msdchallenge.repository.IRepositoryWrapper;
import msdchallenge.repository.OwlimRepositoryWrapper;
import msdchallenge.repository.Parameters;
import msdchallenge.repository.SesameNativeRepositoryWrapper;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a repository containing data from all available files including
 * train_triplets.txt of ~3 Gb size. It may take a long time to run.
 * @author Nikolay
 *
 */
public class RepositoryFiller {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryFiller.class);

	private static final String KAGGLE_DIR = "kaggle" + File.separator;
	private static final String VISIBLE_EVALUATION_TRIPLETS_FILE = 
			KAGGLE_DIR + "kaggle_visible_evaluation_triplets.txt";
	private static final String UNIQUE_TRACKS_FILE = KAGGLE_DIR + "unique_tracks_fixed.txt";
	private static final String KAGGLE_USERS_FILE = KAGGLE_DIR + "kaggle_users.txt";
	private static final String KAGGLE_SONGS_FILE = KAGGLE_DIR + "kaggle_songs.txt";
	private static final String TRAIN_TRIPLETS_FILE = KAGGLE_DIR + "train_triplets.txt";

	private final InputProcessor processor;

	private final RepositoryConnection repositoryConnection;

	public static void main(String[] args) {
		// Parse all the parameters
		Parameters params = new Parameters(args);
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_CONFIG, "config/owlim.ttl");
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_SHOWRESULTS, "true");
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_SHOWSTATS, "false");
		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_UPDATES, "false");
//		params.setDefaultValue(RepositoryWrapper.PARAM_PRELOAD, PREFIX + "preload");
//		params.setDefaultValue(OwlimRepositoryWrapper.PARAM_QUERYFILE, "scooter");

		LOG.info("Using parameters:");
		LOG.info(params.toString());

//		IRepositoryWrapper wrapper = new OwlimRepositoryWrapper(params.getParameters());
		IRepositoryWrapper wrapper = SesameNativeRepositoryWrapper.getTestRepository();

		RepositoryFiller rf = new RepositoryFiller(wrapper.getRepository(), wrapper.getRepositoryConnection());
		rf.fillRepository();
		rf.readListenings();

		wrapper.shutdown();
	}

	public RepositoryFiller(Repository repository, RepositoryConnection connection) {
		if (repository == null) { throw new NullPointerException(); }
		if (connection == null) { throw new NullPointerException(); }
		this.processor =  new InputProcessor(repository.getValueFactory(), connection);
		this.repositoryConnection = connection;
	}

	public void fillRepository() {
		File listenings = new File(VISIBLE_EVALUATION_TRIPLETS_FILE);
		ListeningsFileReader.process(listenings, processor);
		commit();
		
		File tracks = new File(UNIQUE_TRACKS_FILE);
		TracksFileReader.process(tracks, processor);
		commit();
		
		
		File users = new File(KAGGLE_USERS_FILE);
		UsersFileReader.process(users, processor);
		commit();
		
		File songNumbers = new File(KAGGLE_SONGS_FILE);
		SongNumbersFileReader.process(songNumbers, processor);
		commit();
	}

	public void readListenings() {
		File allListenings = new File(TRAIN_TRIPLETS_FILE);
		ListeningsFileReader.process(allListenings, processor);
		commit();
	}

	private void commit() {
		try {
			repositoryConnection.commit();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
