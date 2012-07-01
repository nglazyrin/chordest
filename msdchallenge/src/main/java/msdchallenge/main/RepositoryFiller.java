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

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RepositoryFiller {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryFiller.class);

	private final Repository repository;

	private final RepositoryConnection repositoryConnection;

	private final String prefix;

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

		IRepositoryWrapper m = new OwlimRepositoryWrapper(params.getParameters());
		
		RepositoryFiller rf = new RepositoryFiller(m.getRepository(), m.getRepositoryConnection(), "");
		rf.fillRepository();
		rf.readListenings();

		m.shutdown();
	}

	public RepositoryFiller(Repository repository, RepositoryConnection connection, String prefix) {
		if (repository == null) { throw new NullPointerException(); }
		if (connection == null) { throw new NullPointerException(); }
		if (prefix == null) { throw new NullPointerException(); }
		this.repository = repository;
		this.repositoryConnection = connection;
		this.prefix = prefix;
	}

	public void fillRepository() {
		InputProcessor processor = new InputProcessor(
				repository.getValueFactory(), repositoryConnection);
		
		File listenings = new File(prefix + "kaggle/kaggle_visible_evaluation_triplets.txt");
		ListeningsFileReader.process(listenings, processor);
		commit();
		
		File tracks = new File(prefix + "kaggle/unique_tracks_fixed.txt");
		TracksFileReader.process(tracks, processor);
		commit();
		
		
		File users = new File(prefix + "kaggle/kaggle_users.txt");
		UsersFileReader.process(users, processor);
		commit();
		
		File songNumbers = new File(prefix + "kaggle/kaggle_songs.txt");
		SongNumbersFileReader.process(songNumbers, processor);
		commit();
	}

	public void readListenings() {
		InputProcessor processor = new InputProcessor(
				repository.getValueFactory(), repositoryConnection);
		
		File allListenings = new File(prefix + "kaggle/train_triplets.txt");
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
